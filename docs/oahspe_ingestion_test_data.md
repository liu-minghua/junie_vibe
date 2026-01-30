Perfect — let’s build this in the exact order you requested, Minghua.  
I’ll give you **clean, production‑ready test data builders**, then a **synthetic PDF generator**, and finally a **complete example synthetic PDF** you can drop into your test resources.

Everything will be modular, readable, and aligned with your Oahspe ingestion architecture.

---

# ⭐ 1. **Test Data Builder Classes (Recommended for Unit + Integration Tests)**

These builders eliminate boilerplate and make tests expressive and clean.

Create a package:

```
src/test/java/edu/minghualiu/oahspe/testdata/
```

Then add the following builders.

---

## **BookBuilder.java**

```java
package edu.minghualiu.oahspe.testdata;

import edu.minghualiu.oahspe.entities.Book;

public class BookBuilder {

    private String title = "Test Book";

    public static BookBuilder book() {
        return new BookBuilder();
    }

    public BookBuilder withTitle(String title) {
        this.title = title;
        return this;
    }

    public Book build() {
        return Book.builder()
                .title(title)
                .build();
    }
}
```

---

## **ChapterBuilder.java**

```java
package edu.minghualiu.oahspe.testdata;

import edu.minghualiu.oahspe.entities.Book;
import edu.minghualiu.oahspe.entities.Chapter;

public class ChapterBuilder {

    private String title = "Chapter 1";
    private Book book;

    public static ChapterBuilder chapter() {
        return new ChapterBuilder();
    }

    public ChapterBuilder withTitle(String title) {
        this.title = title;
        return this;
    }

    public ChapterBuilder withBook(Book book) {
        this.book = book;
        return this;
    }

    public Chapter build() {
        Chapter c = Chapter.builder()
                .title(title)
                .book(book)
                .build();

        if (book != null) {
            book.getChapters().add(c);
        }

        return c;
    }
}
```

---

## **VerseBuilder.java**

```java
package edu.minghualiu.oahspe.testdata;

import edu.minghualiu.oahspe.entities.Chapter;
import edu.minghualiu.oahspe.entities.Verse;

public class VerseBuilder {

    private String verseKey = "1/1.1";
    private String text = "Test verse text";
    private Chapter chapter;

    public static VerseBuilder verse() {
        return new VerseBuilder();
    }

    public VerseBuilder withKey(String key) {
        this.verseKey = key;
        return this;
    }

    public VerseBuilder withText(String text) {
        this.text = text;
        return this;
    }

    public VerseBuilder withChapter(Chapter chapter) {
        this.chapter = chapter;
        return this;
    }

    public Verse build() {
        Verse v = Verse.builder()
                .verseKey(verseKey)
                .text(text)
                .chapter(chapter)
                .build();

        if (chapter != null) {
            chapter.getVerses().add(v);
        }

        return v;
    }
}
```

---

## **NoteBuilder.java**

```java
package edu.minghualiu.oahspe.testdata;

import edu.minghualiu.oahspe.entities.Note;
import edu.minghualiu.oahspe.entities.Verse;

public class NoteBuilder {

    private String noteKey = "1";
    private String text = "Test note text";
    private Verse verse;

    public static NoteBuilder note() {
        return new NoteBuilder();
    }

    public NoteBuilder withKey(String key) {
        this.noteKey = key;
        return this;
    }

    public NoteBuilder withText(String text) {
        this.text = text;
        return this;
    }

    public NoteBuilder withVerse(Verse verse) {
        this.verse = verse;
        return this;
    }

    public Note build() {
        Note n = Note.builder()
                .noteKey(noteKey)
                .text(text)
                .verse(verse)
                .build();

        if (verse != null) {
            verse.getNotes().add(n);
        }

        return n;
    }
}
```

---

## **ImageBuilder.java**

```java
package edu.minghualiu.oahspe.testdata;

import edu.minghualiu.oahspe.entities.Image;

public class ImageBuilder {

    private String imageKey = "IMG001";
    private String title = "Test Image";
    private String description = "Test Description";
    private byte[] data = "abc".getBytes();

    public static ImageBuilder image() {
        return new ImageBuilder();
    }

    public ImageBuilder withKey(String key) {
        this.imageKey = key;
        return this;
    }

    public ImageBuilder withTitle(String title) {
        this.title = title;
        return this;
    }

    public ImageBuilder withDescription(String desc) {
        this.description = desc;
        return this;
    }

    public ImageBuilder withData(byte[] data) {
        this.data = data;
        return this;
    }

    public Image build() {
        return Image.builder()
                .imageKey(imageKey)
                .title(title)
                .description(description)
                .data(data)
                .build();
    }
}
```

---

# ⭐ 2. **Synthetic PDF Generator (for System Tests)**

This generator creates small, controlled PDFs for end‑to‑end ingestion tests.

Create:

```
src/test/java/edu/minghualiu/oahspe/testpdf/SyntheticPdfGenerator.java
```

```java
package edu.minghualiu.oahspe.testpdf;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.File;

public class SyntheticPdfGenerator {

    public static void createSimpleOahspePdf(String path) throws Exception {

        try (PDDocument doc = new PDDocument()) {

            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            PDPageContentStream cs = new PDPageContentStream(doc, page);

            cs.setFont(PDType1Font.HELVETICA, 12);

            cs.beginText();
            cs.newLineAtOffset(50, 700);

            cs.showText("Book of Apollo");
            cs.newLineAtOffset(0, -20);

            cs.showText("Chapter 1");
            cs.newLineAtOffset(0, -20);

            cs.showText("14/1.1 In the beginning...");
            cs.newLineAtOffset(0, -20);

            cs.showText("(1) A note about the verse");
            cs.newLineAtOffset(0, -20);

            cs.showText("i001 A synthetic plate");
            cs.endText();

            cs.close();

            doc.save(new File(path));
        }
    }
}
```

This produces a **valid PDF** containing:

- Book title
- Chapter title
- Verse
- Note
- Image reference

Perfect for system tests.

---

# ⭐ 3. **Complete Example Synthetic PDF**

Place this file in:

```
src/test/resources/mini-oahspe.pdf
```

Use the generator:

```java
@Test
void generateMiniPdf() throws Exception {
    SyntheticPdfGenerator.createSimpleOahspePdf("src/test/resources/mini-oahspe.pdf");
}
```

This PDF will contain:

```
Book of Apollo
Chapter 1
14/1.1 In the beginning...
(1) A note about the verse
i001 A synthetic plate
```

This is exactly what your ingestion pipeline expects.

---

# 🎯 What You Have Now

You now have:

### ✔ Full test data builders
Clean, expressive unit + integration tests.

### ✔ Synthetic PDF generator
Deterministic, controlled system tests.

### ✔ Complete example synthetic PDF
A ready‑to‑use file for end‑to‑end ingestion.

This gives you a **professional‑grade testing foundation** for the entire Oahspe ingestion pipeline.

If you want, I can now help you assemble a **Maven/Gradle test profile** that runs unit tests fast and system tests separately.