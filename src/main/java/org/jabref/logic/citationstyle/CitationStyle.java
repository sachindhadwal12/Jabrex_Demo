package org.jabref.logic.citationstyle;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jabref.logic.util.StandardFileType;

import de.undercouch.citeproc.helper.CSLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Representation of a CitationStyle. Stores its name, the file path and the style itself
 */
public class CitationStyle {

    public static final String DEFAULT = "/ieee.csl";

    private static final Logger LOGGER = LoggerFactory.getLogger(CitationStyle.class);
    private static final String STYLES_ROOT = "/csl-styles";
    private static final List<CitationStyle> STYLES = new ArrayList<>();
    private static final DocumentBuilderFactory FACTORY = DocumentBuilderFactory.newInstance();

    private final String filePath;
    private final String title;
    private final String source;

    private CitationStyle(final String filename, final String title, final String source) {
        this.filePath = Objects.requireNonNull(filename);
        this.title = Objects.requireNonNull(title);
        this.source = Objects.requireNonNull(source);
    }

    /**
     * Creates an CitationStyle instance out of the style string
     */
    private static Optional<CitationStyle> createCitationStyleFromSource(final String source, final String filename) {
        if ((filename != null) && !filename.isEmpty() && (source != null) && !source.isEmpty()) {
            try {
                InputSource inputSource = new InputSource();
                inputSource.setCharacterStream(new StringReader(stripInvalidProlog(source)));

                Document doc = FACTORY.newDocumentBuilder().parse(inputSource);

                // See CSL#canFormatBibliographies, checks if the tag exists
                NodeList bibs = doc.getElementsByTagName("bibliography");
                if (bibs.getLength() <= 0) {
                    LOGGER.debug("no bibliography element for file {} ", filename);
                    return Optional.empty();
                }

                NodeList nodes = doc.getElementsByTagName("info");
                NodeList titleNode = ((Element) nodes.item(0)).getElementsByTagName("title");
                String title = ((CharacterData) titleNode.item(0).getFirstChild()).getData();

                return Optional.of(new CitationStyle(filename, title, source));
            } catch (ParserConfigurationException | SAXException | IOException e) {
                LOGGER.error("Error while parsing source", e);
            }
        }
        return Optional.empty();
    }

    private static String stripInvalidProlog(String source) {
        int startIndex = source.indexOf("<");
        if (startIndex > 0) {
            return source.substring(startIndex);
        } else {
            return source;
        }
    }

    /**
     * Loads the CitationStyle from the given file
     */
    public static Optional<CitationStyle> createCitationStyleFromFile(final String styleFile) {
        if (!isCitationStyleFile(styleFile)) {
            LOGGER.error("Can only load style files: {}", styleFile);
            return Optional.empty();
        }

        try {
            String text;
            String internalFile = STYLES_ROOT + (styleFile.startsWith("/") ? "" : "/") + styleFile;
            URL url = CitationStyle.class.getResource(internalFile);

            if (url != null) {
                text = CSLUtils.readURLToString(url, StandardCharsets.UTF_8.toString());
            } else {
                // if the url is null then the style is located outside the classpath
                text = Files.readString(Path.of(styleFile));
            }
            return createCitationStyleFromSource(text, styleFile);
        } catch (NoSuchFileException e) {
            LOGGER.error("Could not find file: {}", styleFile, e);
        } catch (IOException e) {
            LOGGER.error("Error reading source file", e);
        }
        return Optional.empty();
    }

    /**
     * Provides the default citation style which is currently IEEE
     *
     * @return default citation style
     */
    public static CitationStyle getDefault() {
        return createCitationStyleFromFile(DEFAULT).orElse(new CitationStyle("", "Empty", ""));
    }

    /**
     * Provides the citation styles that come with JabRef.
     *
     * @return list of available citation styles
     */
    public static List<CitationStyle> discoverCitationStyles() {
        if (!STYLES.isEmpty()) {
            return STYLES;
        }

        URL url = CitationStyle.class.getResource(STYLES_ROOT + "/acm-siggraph.csl");
        Objects.requireNonNull(url);

        try {
            URI uri = url.toURI();
            Path path = Path.of(uri).getParent();
            STYLES.addAll(discoverCitationStylesInPath(path));

            return STYLES;
        } catch (URISyntaxException | IOException e) {
            LOGGER.error("something went wrong while searching available CitationStyles", e);
            return Collections.emptyList();
        }
    }

    private static List<CitationStyle> discoverCitationStylesInPath(Path path) throws IOException {
        try (Stream<Path> stream = Files.find(path, 1, (file, attr) -> file.toString().endsWith("csl"))) {
            return stream.map(Path::getFileName)
                         .map(Path::toString)
                         .map(CitationStyle::createCitationStyleFromFile)
                         .filter(Optional::isPresent)
                         .map(Optional::get)
                         .collect(Collectors.toList());
        }
    }

    /**
     * Checks if the given style file is a CitationStyle
     */
    public static boolean isCitationStyleFile(String styleFile) {
        return StandardFileType.CITATION_STYLE.getExtensions().stream().anyMatch(styleFile::endsWith);
    }

    public String getTitle() {
        return title;
    }

    public String getSource() {
        return source;
    }

    public String getFilePath() {
        return filePath;
    }

    @Override
    public String toString() {
        return title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        CitationStyle other = (CitationStyle) o;
        return Objects.equals(source, other.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source);
    }
}
