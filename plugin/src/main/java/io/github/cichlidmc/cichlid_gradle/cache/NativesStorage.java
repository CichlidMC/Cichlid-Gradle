package io.github.cichlidmc.cichlid_gradle.cache;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;

import io.github.cichlidmc.cichlid_gradle.util.FileUtils;
import io.github.cichlidmc.pistonmetaparser.FullVersion;
import io.github.cichlidmc.pistonmetaparser.version.library.Classifier;
import io.github.cichlidmc.pistonmetaparser.version.library.Library;
import io.github.cichlidmc.pistonmetaparser.version.library.Natives;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

public class NativesStorage {
	public static final String TEMP_JAR = "temp-jar-for-extracting.jar";

	private static final Logger logger = Logging.getLogger(AssetStorage.class);

	private final Path root;

	NativesStorage(Path root) {
		this.root = root;
	}

	void extractNatives(FullVersion version) throws IOException {
		Path dir = this.root.resolve(version.id);

		for (Library library : version.libraries) {
			Optional<Classifier> maybeClassifier = library.natives.flatMap(Natives::choose);
			if (maybeClassifier.isPresent()) {
				Classifier classifier = maybeClassifier.get();
				logger.quiet("Extracting natives for library {}: classifier {}", library.name, classifier.name);
				this.downloadAndExtract(classifier, dir);
			}
		}
	}

	private void downloadAndExtract(Classifier classifier, Path dir) throws IOException {
		Path tempJar = dir.resolve(TEMP_JAR);
		FileUtils.downloadSilently(classifier.artifact, tempJar);

		try (FileSystem fs = FileSystems.newFileSystem(tempJar)) {
			// jar should have 1 root
			Path root = fs.getRootDirectories().iterator().next();
			try (Stream<Path> stream = Files.list(root)) {
				// the version manifest specifies files that should be excluded from extraction,
				// but it's undocumented and a pain to handle, and it doesn't even matter in the end
				// as far as I know.
				for (Iterator<Path> itr = stream.iterator(); itr.hasNext();) {
					Path file = itr.next();
					Path dest = dir.resolve(file.getFileName().toString());
					Files.copy(file, dest);
				}
			}
		}

		Files.delete(tempJar);
	}
}
