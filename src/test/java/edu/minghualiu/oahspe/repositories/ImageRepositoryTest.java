package edu.minghualiu.oahspe.repositories;

import edu.minghualiu.oahspe.entities.Image;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
class ImageRepositoryTest {

    @Autowired
    private ImageRepository imageRepository;

    @Test
    void saveAndFindImageByKey() {
        Image image = Image.builder()
                .imageKey("i001")
                .title("Illustration 1")
                .description("Description")
                .contentType("image/png")
                .data(new byte[]{1, 2, 3})
                .build();

        imageRepository.save(image);

        Optional<Image> found = imageRepository.findByImageKey("i001");

        assertThat(found).isPresent();
        assertThat(found.get().getData()).isNotEmpty();
    }
}

