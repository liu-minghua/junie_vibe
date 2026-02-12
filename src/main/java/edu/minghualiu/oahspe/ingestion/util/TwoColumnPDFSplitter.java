package edu.minghualiu.oahspe.ingestion.util;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Splits two-column PDF text into separate left and right columns,
 * handling verses, footnotes, illustrations, and footnote spillover.
 */
@Slf4j
public class TwoColumnPDFSplitter {

    // Configuration
    private static final int MIN_ILLUSTRATION_TEXT_LENGTH = 300;

    private static final int COLUMN_GUTTER_MIN_SPACES = 40;

    private static final Pattern FOOTNOTE_REF_PATTERN =
            Pattern.compile("\\s(\\d{1,4})\\s*(?:$|\\n)", Pattern.MULTILINE);

    // Candidate: marker on its own line
    @SuppressWarnings("unused")
    private static final Pattern FOOTNOTE_MARKER_LINE =
            Pattern.compile("^(\\d+|\\*+)\\s*$", Pattern.MULTILINE);

    // Definition start: marker line, then an indented non-empty line (must be the next line).
    // NOTE: Use [ \t]+ instead of \\s+ because \\s also matches newlines and would skip blank lines.
    private static final Pattern FOOTNOTE_DEFINITION_START =
            Pattern.compile("^(\\d+|\\*+)\\s*\\R[ \\t]+\\S", Pattern.MULTILINE);

    private static final Pattern ASTERISK_REF_PATTERN =
            Pattern.compile("\\s(\\*+)\\s*(?:$|\\n)", Pattern.MULTILINE);

    private static Optional<Integer> findColumnGutterPos(String text) {
        if (text == null || text.isEmpty()) return Optional.empty();
        Pattern gutter = Pattern.compile("(?m)^\\s{" + COLUMN_GUTTER_MIN_SPACES + ",}\\R");
        Matcher m = gutter.matcher(text);
        return m.find() ? Optional.of(m.start()) : Optional.empty();
    }

    /**
     * Result container for split columns.
     */
    public static class SplitResult {
        public final String leftVerses;
        public final String leftFootnotes;
        public final String rightVerses;
        public final String rightFootnotes;

        public SplitResult(String leftVerses, String leftFootnotes,
                           String rightVerses, String rightFootnotes) {
            this.leftVerses = leftVerses != null ? leftVerses : "";
            this.leftFootnotes = leftFootnotes != null ? leftFootnotes : "";
            this.rightVerses = rightVerses != null ? rightVerses : "";
            this.rightFootnotes = rightFootnotes != null ? rightFootnotes : "";
        }

        public boolean hasLeftContent() {
            return !leftVerses.isEmpty() || !leftFootnotes.isEmpty();
        }

        public boolean hasRightContent() {
            return !rightVerses.isEmpty() || !rightFootnotes.isEmpty();
        }

        @Override
        public String toString() {
            return String.format(
                    "=== LEFT COLUMN ===%nVerses:%n%s%n%nFootnotes:%n%s%n%n" +
                            "=== RIGHT COLUMN ===%nVerses:%n%s%n%nFootnotes:%n%s",
                    leftVerses, leftFootnotes, rightVerses, rightFootnotes
            );
        }
    }

    /**
     * Main entry point for splitting two-column text.
     */
    public static SplitResult split(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return new SplitResult("", "", "", "");
        }

        try {
            return splitInternal(rawText);
        } catch (Exception e) {
            // Fallback: return all text as left verses on error
            log.error("Error splitting columns: {}", e.getMessage());
            return new SplitResult(rawText.trim(), "", "", "");
        }
    }

    private static SplitResult splitInternal(String rawText) {
        // Step 1: Find where footnotes section starts
        Optional<Integer> footnotesStartPos = findFootnotesStart(rawText);

        if (footnotesStartPos.isEmpty()) {
            // No footnotes: check for illustration as split point
            return handleNoFootnotesCase(rawText);
        }

        int fnStart = footnotesStartPos.get();

        // Step 2: Separate verses from footnotes + rest
        String versesSection = rawText.substring(0, fnStart).trim();
        String afterVerses = rawText.substring(fnStart);

        // Step 3: Find illustration marker in afterVerses
        Optional<Integer> illustrationPos = findIllustrationMarker(afterVerses);

        String footnotesSection;
        String rightSection = "";

        if (illustrationPos.isPresent()) {
            int illPos = illustrationPos.get();
            footnotesSection = afterVerses.substring(0, illPos).trim();
            rightSection = afterVerses.substring(illPos).trim();
        } else {
            footnotesSection = afterVerses.trim();
        }

        Optional<Integer> gutterPos = findColumnGutterPos(footnotesSection);
        if (gutterPos.isPresent()) {
            int pos = gutterPos.get();
            String leftFootnotesText = footnotesSection.substring(0, pos).trim();
            String spilledRight = footnotesSection.substring(pos).trim();

            footnotesSection = leftFootnotesText;

            rightSection = spilledRight.isEmpty()
                    ? rightSection
                    : (rightSection.isEmpty() ? spilledRight : (spilledRight + "\n" + rightSection));
        }

        List<Footnote> leftFootnotes = extractFootnotes(footnotesSection);

        // Step 5: Parse right section
        RightSectionParts rightParts = parseRightSection(rightSection);

        // Step 6: Find split point in verses
        Optional<String> splitMarker = detectSplitPoint(versesSection, leftFootnotes);

        // Step 7: Split verses
        VerseSplit versesSplit = splitVerses(versesSection, splitMarker);

        // Step 8: Combine right verses
        String rightVerses = combineRightVerses(versesSplit.rightPart, rightParts.verses);

        // Step 9: Format output
        String leftFN = formatFootnotes(leftFootnotes);
        String rightFN = rightParts.footnotes;

        return new SplitResult(
                versesSplit.leftPart,
                leftFN,
                rightVerses,
                rightFN
        );
    }

    private static SplitResult handleNoFootnotesCase(String rawText) {
        Optional<Integer> illustrationPos = findIllustrationMarker(rawText);
        if (illustrationPos.isPresent()) {
            int pos = illustrationPos.get();
            return new SplitResult(
                    rawText.substring(0, pos).trim(),
                    "",
                    rawText.substring(pos).trim(),
                    ""
            );
        }
        return new SplitResult(rawText.trim(), "", "", "");
    }

    /**
     * Find where the footnotes section starts in the raw text.
     *
     * Strategy:
     * - Look for a blank line that separates verses from footnotes.
     * - The footnotes section has distinctive characteristics:
     *   - Contains footnote marker lines (numbers or asterisks on their own line)
     *   - Has at least 2+ footnote definitions following
     */
    private static Optional<Integer> findFootnotesStart(String text) {
        if (text == null || text.isEmpty()) {
            return Optional.empty();
        }

        String[] lines = text.split("\\R", -1);

        int bestStartLine = -1;
        int bestScore = 0;

        // Scan for blank line transitions that might separate verses from footnotes
        for (int i = 0; i < lines.length - 1; i++) {
            String currentLine = lines[i];

            // Look for blank lines as potential section boundaries
            if (!isBlankLike(currentLine)) {
                continue;
            }

            // The next non-blank line after this blank line is a candidate
            int nextNonBlank = findNextNonBlankLine(lines, i + 1);
            if (nextNonBlank < 0) {
                continue; // No more content
            }

            int score = scoreAsFootnotesStart(lines, nextNonBlank);
            if (score > bestScore) {
                bestScore = score;
                bestStartLine = nextNonBlank;
            }
        }

        // Also check line 0 (footnotes might start at beginning; no verses)
        if (lines.length > 0) {
            int score = scoreAsFootnotesStart(lines, 0);
            if (score > bestScore) {
                bestScore = score;
                bestStartLine = 0;
            }
        }

        // Require at least minimal confidence
        if (bestScore < 2) {
            return Optional.empty();
        }

        // Convert line index back to character position (assume '\n' per line)
        int charPos = 0;
        for (int i = 0; i < bestStartLine && i < lines.length; i++) {
            charPos += lines[i].length() + 1;
        }

        if (log.isDebugEnabled()) {
            log.debug("=== FOOTNOTE START DETECTION ===");
            log.debug("Best start line: {}", bestStartLine);
            log.debug("Best score: {}", bestScore);
            if (bestStartLine >= 0 && bestStartLine < lines.length) {
                log.debug("Context:");
                dumpLines("  ", lines, bestStartLine, 3);
            }
            log.debug("================================");
        }

        return Optional.of(charPos);
    }

    /**
     * Find the next non-blank line starting from the given index.
     */
    private static int findNextNonBlankLine(String[] lines, int startFrom) {
        for (int i = startFrom; i < lines.length; i++) {
            if (!isBlankLike(lines[i])) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Like {@link String#isBlank()}, but also treats Unicode space separators as blank.
     */
    private static boolean isBlankLike(String s) {
        if (s == null) {
            return true;
        }

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!Character.isWhitespace(c) && !Character.isSpaceChar(c)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Score a line as the potential start of the footnotes section.
     * A good footnotes section has:
     * 1) Multiple footnote marker lines (numbers or asterisks)
     * 2) Footnote definitions (marker followed by indented/definition text)
     * 3) Not verse-numbered content
     */
    private static int scoreAsFootnotesStart(String[] lines, int startLine) {
        if (startLine < 0 || startLine >= lines.length) {
            return 0;
        }

        int score = 0;
        int maxLinesToCheck = Math.min(startLine + 40, lines.length);

        int footnoteDefsFound = 0;
        boolean hasVerseNumbers = false;

        for (int i = startLine; i < maxLinesToCheck; i++) {
            String line = lines[i];

            if (isVerseNumberLine(line)) {
                hasVerseNumbers = true;
                break;
            }

            if (isFootnoteMarkerLine(line)) {
                boolean hasDefinition = hasFootnoteDefinitionAfter(lines, i, 5);

                if (hasDefinition) {
                    footnoteDefsFound++;
                    score += 2;
                }

                if (footnoteDefsFound >= 3) {
                    score += 2;
                    break;
                }
            }
        }

        if (hasVerseNumbers) {
            score = 0;
        }

        return score;
    }

    /**
     * Check if there's a footnote definition within N lines after a marker.
     */
    private static boolean hasFootnoteDefinitionAfter(String[] lines, int markerLine, int lookAhead) {
        int endLine = Math.min(markerLine + lookAhead, lines.length);

        for (int i = markerLine + 1; i < endLine; i++) {
            String line = lines[i];

            if (isBlankLike(line)) {
                continue;
            }

            if (isFootnoteMarkerLine(line)) {
                return false;
            }

            if (startsIndented(line)) {
                return true;
            }

            if (isFootnoteDefinitionStart(line)) {
                return true;
            }

            return false;
        }

        return false;
    }

    /**
     * Check if a line is a verse number line (e.g., "05/12.1. When it was...").
     */
    private static boolean isVerseNumberLine(String line) {
        if (line == null) {
            return false;
        }
        String trimmed = line.trim();
        return trimmed.matches("^\\d{2}/\\d+\\.\\d+\\..*");
    }

    private static boolean isLineBreak(char c) {
        return c == '\n' || c == '\r' || c == '\f' || c == '\u2028' || c == '\u2029';
    }

    /**
     * Check if a line is a footnote marker line (just a number or asterisks on its own).
     */
    private static boolean isFootnoteMarkerLine(String line) {
        if (line == null) {
            return false;
        }

        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return false;
        }

        // Numeric footnote marker (1–4 digits only)
        if (trimmed.matches("^\\d{1,4}$")) {
            return true;
        }

        // Asterisk footnote marker
        return trimmed.matches("^\\*+$");
    }

    /**
     * Check if a line starts with indentation.
     */
    private static boolean startsIndented(String line) {
        if (line == null || line.isEmpty()) {
            return false;
        }

        // Footnote continuation is usually slightly indented.
        // Treating any indentation as continuation causes verse text to be swallowed.
        final int maxFootnoteIndentSpaces = 12;

        int indent = leadingIndentColumns(line);
        return indent > 0 && indent <= maxFootnoteIndentSpaces;
    }

    /**
     * Counts leading indentation in "columns": space = 1, tab = 4 (approx).
     */
    private static int leadingIndentColumns(String line) {
        int cols = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == ' ') {
                cols += 1;
                continue;
            }
            if (c == '\t') {
                cols += 4;
                continue;
            }
            break;
        }
        return cols;
    }

    private static boolean hasVerseNumberAhead(String[] lines, int fromIndex, int lookAhead) {
        int end = Math.min(lines.length, fromIndex + lookAhead + 1);
        for (int i = fromIndex; i < end; i++) {
            if (isVerseNumberLine(lines[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a line starts a footnote definition (common patterns).
     */
    private static boolean isFootnoteDefinitionStart(String line) {
        if (line == null || line.isBlank()) {
            return false;
        }

        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return false;
        }

        if (trimmed.matches("^\\d{1,4}$") || trimmed.matches("^\\*+$")) {
            return false;
        }

        if (trimmed.matches("^\\d{2}/\\d+\\.\\d+\\..*")) {
            return false;
        }

        String lower = trimmed.toLowerCase();

        return lower.startsWith("i.e.")
                || lower.startsWith("e.g.")
                || lower.startsWith("see ")
                || lower.startsWith("cf.")
                || lower.startsWith("that is")
                || lower.matches("^[–—-]\\s*ed\\..*")
                || lower.matches("^[–—-]\\s*\\d{4}.*");
    }

    @SuppressWarnings("unused")
    private static int scoreFootnoteStartCandidate(String text, int candidatePos) {
        final int windowSize = 1200;
        int end = Math.min(text.length(), candidatePos + windowSize);
        String window = text.substring(candidatePos, end);

        // Only treat this candidate as "footnotes start" if a definition starts right here.
        Matcher def = FOOTNOTE_DEFINITION_START.matcher(window);
        if (!def.lookingAt()) {
            return 0;
        }

        int count = 0;
        while (def.find()) {
            count++;
            if (count >= 6) break;
        }
        return count;
    }

    private static Optional<Integer> findIllustrationMarker(String text) {
        Pattern illustrationPattern = Pattern.compile("\\bi00\\d\\b|\\bi0\\d{2}\\b");
        Matcher matcher = illustrationPattern.matcher(text);
        return matcher.find() ? Optional.of(matcher.start()) : Optional.empty();
    }

    private static class Footnote {
        final String marker;
        final String text;
        final boolean isNumeric;
        final int numericValue;

        Footnote(String marker, String text) {
            this.marker = marker;
            this.text = text;
            this.isNumeric = marker.matches("\\d+");
            this.numericValue = isNumeric ? Integer.parseInt(marker) : -1;
        }
    }

    private static List<Footnote> extractFootnotes(String footnotesSection) {
        List<Footnote> footnotes = new ArrayList<>();
        if (footnotesSection == null || footnotesSection.isBlank()) {
            return footnotes;
        }

        Pattern markerOnly = Pattern.compile("^\\s*(\\d+|\\*+)\\s*$");
        Pattern verseNumber = Pattern.compile("^\\s*\\d{2}/\\d{2}\\.\\d+\\..*");

        final int shortWrapLineMax = 25;

        String[] lines = footnotesSection.split("\\R", -1);

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            Matcher m = markerOnly.matcher(line);
            if (!m.matches()) {
                continue;
            }

            String marker = m.group(1);

            StringBuilder body = new StringBuilder();
            String lastCapturedTrim = null;

            int k = i + 1;
            while (k < lines.length && lines[k].isBlank()) k++;

            String stopReason = "END_OF_SECTION";
            int stopAtLineIndex = Math.min(k, lines.length);

            while (k < lines.length) {
                String cur = lines[k];

                if (verseNumber.matcher(cur).matches()) {
                    stopReason = "HIT_VERSE_NUMBER_LINE";
                    stopAtLineIndex = k;
                    break;
                }

                if (markerOnly.matcher(cur).matches()) {
                    stopReason = "HIT_NEXT_MARKER_LINE";
                    stopAtLineIndex = k;
                    break;
                }

                if (!cur.isBlank()) {
                    String curTrim = cur.trim();

                    boolean definitionKeyword =
                            startsWithIgnoreCase(curTrim, "glossary.")
                                    || startsWithIgnoreCase(curTrim, "note.")
                                    || startsWithIgnoreCase(curTrim, "note:")
                                    || startsWithIgnoreCase(curTrim, "cf.")
                                    || startsWithIgnoreCase(curTrim, "see ")
                                    || curTrim.matches("^[–—-]\\s*Ed\\..*");

                    // If verses restart soon after this line, we’re likely leaking into verse text.
                    if (!curTrim.isEmpty() && !definitionKeyword && hasVerseNumberAhead(lines, k, 8)) {
                        stopReason = "VERSE_RESTART_AHEAD (likely verse spillover)";
                        stopAtLineIndex = k;
                        break;
                    }

                    // Spillover guard (only for non-indented narrative lines).
                    if (!startsIndented(cur)) {
                        if (endsSentence(lastCapturedTrim) && !definitionKeyword) {
                            stopReason = "UNINDENTED_NARRATIVE_AFTER_SENTENCE_END (likely spillover)";
                            stopAtLineIndex = k;
                            break;
                        }

                        int t = k + 1;
                        while (t < lines.length && lines[t].isBlank()) t++;
                        if (t < lines.length && markerOnly.matcher(lines[t]).matches()) {
                            if (curTrim.length() > shortWrapLineMax && !definitionKeyword) {
                                stopReason = "UNINDENTED_PROSE_BEFORE_NEXT_MARKER (likely spillover)";
                                stopAtLineIndex = k;
                                break;
                            }
                        }
                    }
                }

                if (!cur.isBlank()) {
                    String curTrim = cur.trim();
                    if (!body.isEmpty()) body.append('\n');
                    body.append(curTrim);
                    lastCapturedTrim = curTrim;
                }

                k++;
            }

            if (!body.isEmpty()) {
                footnotes.add(new Footnote(marker, body.toString()));
            }

            if (log.isDebugEnabled()) {
                log.debug("=== FOOTNOTE DEBUG ===");
                log.debug("marker={}", marker);
                log.debug("startLineIndex={}", i);
                log.debug("capturedLines={}", countNonBlankLines(body.toString()));
                log.debug("stopReason={}", stopReason);
                dumpLines("stopContext", lines, stopAtLineIndex, 2);
                log.debug("======================");
            }

            i = k - 1;
        }

        return footnotes;
    }

    private static boolean endsSentence(String s) {
        if (s == null) return false;
        int i = s.length() - 1;
        while (i >= 0 && Character.isWhitespace(s.charAt(i))) i--;
        if (i < 0) return false;
        char c = s.charAt(i);
        return c == '.' || c == '!' || c == '?';
    }

    private static boolean startsWithIgnoreCase(String s, String prefix) {
        if (s == null || prefix == null) return false;
        if (prefix.length() > s.length()) return false;
        return s.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    private static int countNonBlankLines(String s) {
        if (s == null || s.isBlank()) return 0;
        int count = 0;
        for (String line : s.split("\\R", -1)) {
            if (!line.isBlank()) count++;
        }
        return count;
    }

    private static void dumpLines(String label, String[] lines, int center, int radius) {
        int from = Math.max(0, center - radius);
        int to = Math.min(lines.length - 1, center + radius);

        log.debug("{}: lineIndex={} (showing {}..{})", label, center, from, to);
        for (int idx = from; idx <= to; idx++) {
            String visible = lines[idx]
                    .replace("\t", "\\t")
                    .replace("\r", "\\r");
            log.debug("  [{}] \"{}\"", idx, visible);
        }
    }

    private static class RightSectionParts {
        final String verses;
        final String footnotes;

        RightSectionParts(String verses, String footnotes) {
            this.verses = verses != null ? verses : "";
            this.footnotes = footnotes != null ? footnotes : "";
        }
    }

    private static RightSectionParts parseRightSection(String rightSection) {
        if (rightSection.isEmpty()) {
            return new RightSectionParts("", "");
        }

        Optional<Integer> rightFNStart = findRightFootnotesStart(rightSection);

        if (rightFNStart.isPresent()) {
            int pos = rightFNStart.get();
            return new RightSectionParts(
                    rightSection.substring(0, pos).trim(),
                    rightSection.substring(pos).trim()
            );
        }

        return new RightSectionParts(rightSection, "");
    }

    private static Optional<Integer> findRightFootnotesStart(String text) {
        String[] lines = text.split("\\R", -1);
        StringBuilder processed = new StringBuilder();
        int charCount = 0;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            processed.append(line).append('\n');
            charCount += line.length();

            // After substantial content, look for footnote continuation
            if (charCount > MIN_ILLUSTRATION_TEXT_LENGTH && i < lines.length - 1) {
                String nextLine = lines[i + 1].trim();

                // Check if next line is footnote continuation
                if (!nextLine.matches("^\\d{2}/.*") &&     // Not a verse
                        !nextLine.matches("^i\\d{3,4}.*") && // Not an illustration
                        !nextLine.matches("^\\d+\\s+.*") &&  // Not a new footnote
                        !nextLine.isEmpty()) {

                    return Optional.of(processed.length());
                }
            }
        }

        return Optional.empty();
    }

    private static Optional<String> detectSplitPoint(String versesSection, List<Footnote> leftFootnotes) {
        Optional<String> lastRef = findLastFootnoteReference(versesSection);
        if (lastRef.isPresent()) {
            return lastRef;
        }

        if (!leftFootnotes.isEmpty()) {
            return Optional.of(leftFootnotes.get(leftFootnotes.size() - 1).marker);
        }

        return Optional.empty();
    }

    private static Optional<String> findLastFootnoteReference(String text) {
        String lastRef = null;
        int lastPos = -1;

        Matcher numMatcher = FOOTNOTE_REF_PATTERN.matcher(text);
        while (numMatcher.find()) {
            if (numMatcher.start() > lastPos) {
                lastPos = numMatcher.start();
                lastRef = numMatcher.group(1);
            }
        }

        Matcher asteriskMatcher = ASTERISK_REF_PATTERN.matcher(text);
        while (asteriskMatcher.find()) {
            if (asteriskMatcher.start() > lastPos) {
                lastPos = asteriskMatcher.start();
                lastRef = asteriskMatcher.group(1);
            }
        }

        return Optional.ofNullable(lastRef);
    }

    private static class VerseSplit {
        final String leftPart;
        final String rightPart;

        VerseSplit(String left, String right) {
            this.leftPart = left != null ? left : "";
            this.rightPart = right != null ? right : "";
        }
    }

    private static VerseSplit splitVerses(String versesSection, Optional<String> splitMarker) {
        if (splitMarker.isEmpty()) {
            return new VerseSplit(versesSection, "");
        }

        String marker = splitMarker.get();
        Pattern pattern = Pattern.compile(
                "\\s" + Pattern.quote(marker) + "\\s*$",
                Pattern.MULTILINE
        );
        Matcher matcher = pattern.matcher(versesSection);

        int splitPos = -1;
        while (matcher.find()) {
            splitPos = matcher.end();
        }

        if (splitPos > 0 && splitPos < versesSection.length()) {
            return new VerseSplit(
                    versesSection.substring(0, splitPos).trim(),
                    versesSection.substring(splitPos).trim()
            );
        }

        return new VerseSplit(versesSection, "");
    }

    private static String combineRightVerses(String versesFromSplit, String versesFromRight) {
        if (!versesFromSplit.isEmpty() && !versesFromRight.isEmpty()) {
            return versesFromSplit + "\n" + versesFromRight;
        }
        return !versesFromSplit.isEmpty() ? versesFromSplit : versesFromRight;
    }

    private static String formatFootnotes(List<Footnote> footnotes) {
        if (footnotes.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < footnotes.size(); i++) {
            sb.append(footnotes.get(i).text);
            if (i < footnotes.size() - 1) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    /**
     * Batch process multiple pages.
     */
    public static List<SplitResult> splitMultiplePages(List<String> pages) {
        List<SplitResult> results = new ArrayList<>();
        for (String page : pages) {
            results.add(split(page));
        }
        return results;
    }

    /**
     * Split and return as JSON-like map for easy integration.
     */
    public static java.util.Map<String, String> splitToMap(String rawText) {
        SplitResult result = split(rawText);
        java.util.Map<String, String> map = new java.util.HashMap<>();
        map.put("left_verses", result.leftVerses);
        map.put("left_footnotes", result.leftFootnotes);
        map.put("right_verses", result.rightVerses);
        map.put("right_footnotes", result.rightFootnotes);
        return map;
    }

    /**
     * Validate split result.
     */
    public static boolean isValidSplit(SplitResult result) {
        return result != null && (result.hasLeftContent() || result.hasRightContent());
    }

    /**
     * Prints N lines before and after the line containing pos, including the exact code points.
     */
    private static void dumpLinesAroundPos(String text, int pos, int radius) {
        int[] lineStarts = computeLineStarts(text);
        int lineIndex = findLineIndex(lineStarts, pos);

        int from = Math.max(0, lineIndex - radius);
        int to = Math.min(lineStarts.length - 1, lineIndex + radius);

        for (int li = from; li <= to; li++) {
            int start = lineStarts[li];
            int end = (li + 1 < lineStarts.length) ? lineStarts[li + 1] : text.length();

            int contentEnd = end;
            while (contentEnd > start && isLineBreak(text.charAt(contentEnd - 1))) {
                contentEnd--;
            }

            String line = text.substring(start, contentEnd);
            log.debug("LINE[{}] range [{},{}) len={}", li, start, contentEnd, line.length());
            dumpSingleLine("  text", line);
        }
    }

    private static void dumpSingleLine(String label, String line) {
        String visible = line
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
        log.debug("{}=\"{}\"", label, visible);

        StringBuilder sb = new StringBuilder();
        sb.append(label).append(" codepoints: ");
        if (line.isEmpty()) {
            sb.append("(empty)");
            log.debug(sb.toString());
            return;
        }

        for (int i = 0; i < line.length(); ) {
            int cp = line.codePointAt(i);
            String name;
            try {
                name = Character.getName(cp);
            } catch (IllegalArgumentException e) {
                name = "UNKNOWN";
            }
            sb.append(String.format("U+%04X(%s) ", cp, name));
            i += Character.charCount(cp);
        }
        log.debug(sb.toString());
    }

    private static int[] computeLineStarts(String text) {
        int[] tmp = new int[text.length() + 1];
        int count = 0;
        tmp[count++] = 0;

        for (int i = 0; i < text.length(); i++) {
            if (isLineBreak(text.charAt(i))) {
                int next = i + 1;
                if (next < text.length()) {
                    tmp[count++] = next;
                }
            }
        }

        int[] out = new int[count];
        System.arraycopy(tmp, 0, out, 0, count);
        return out;
    }

    private static int findLineIndex(int[] lineStarts, int pos) {
        int lo = 0, hi = lineStarts.length - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int s = lineStarts[mid];
            if (s == pos) return mid;
            if (s < pos) lo = mid + 1;
            else hi = mid - 1;
        }
        return Math.max(0, hi);
    }
}