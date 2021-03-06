package de.charite.compbio.exomiser.db.parsers;

import de.charite.compbio.exomiser.db.resources.Resource;
import de.charite.compbio.exomiser.db.resources.ResourceOperationStatus;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parse the permissive enhancers file from Fantom
 *
 * @version 7.02 (14th December 2015)
 * @author Damian Smedley
 */
public class FantomEnhancerParser implements ResourceParser {

    private static final Logger logger = LoggerFactory.getLogger(FantomEnhancerParser.class);

    /**
     * @param conn COnnection to the Exomiser database.
     */
    public FantomEnhancerParser() {
    }

    /**
     * This function does the actual work of parsing the HPO file.
     *
     * @param resource
     * @param inDir Complete path to directory containing the
     * human-phenotype-ontology.obo or hp.obo file.
     * @param outDir Directory where output file is to be written
     * @return
     */
    @Override
    public void parseResource(Resource resource, Path inDir, Path outDir) {

        Path inFile = inDir.resolve(resource.getExtractedFileName());
        Path outFile = outDir.resolve(resource.getParsedFileName());

        logger.info("Parsing {} file: {}. Writing out to: {}", resource.getName(), inFile, outFile);
        ResourceOperationStatus status;

        try (BufferedReader reader = Files.newBufferedReader(inFile, Charset.forName("UTF-8"));
                BufferedWriter writer = Files.newBufferedWriter(outFile, Charset.defaultCharset())) {
            // read in array of tissues from header  
            String line = reader.readLine();
            String[] tissues = line.split(",");
            while ((line = reader.readLine()) != null) {
                line = line.replaceAll("\n", "");// ? strip off newline first
                String[] lineParts = line.split(",");
                int i = 0;
                String chr = "";
                String start = "";
                String end = "";
                for (String linePart : lineParts) {
                    if (i == 0) {
                        linePart = linePart.replaceAll("\"", "");
                        String[] parts = linePart.split(":");
                        chr = parts[0];
                        chr = chr.replace("chr", "");
                        if (chr.equals("X")) {
                            chr = "23";
                        } else if (chr.equals("y")) {
                            chr = "24";
                        }
                        String[] positions = parts[1].split("-");
                        start = positions[0];
                        end = positions[1];

                    } else if (linePart.equals("1")) {
                        writer.write(String.format("%s|%s|%s|%s", chr, start, end, "FANTOM permissive"));
                        writer.newLine();
                        break;
                        // if want to reinstate tissue-specific parts in table then add a row for every tissue instead
//                        String tissue = tissues[i];
//                        tissue = tissue.replace("\"", "");
//                        writer.write(String.format("%s|%s|%s|%s|%s", chr, start, end, "FANTOM permissive", tissue));
//                        writer.newLine();
                    }
                    i++;
                }
            }
            writer.close();
            reader.close();
            status = ResourceOperationStatus.SUCCESS;
        } catch (FileNotFoundException ex) {
            logger.error(null, ex);
            status = ResourceOperationStatus.FILE_NOT_FOUND;
        } catch (IOException ex) {
            logger.error(null, ex);
            status = ResourceOperationStatus.FAILURE;
        }

        resource.setParseStatus(status);

        logger.info(
                "{}", status);
    }

}
/* eof */
