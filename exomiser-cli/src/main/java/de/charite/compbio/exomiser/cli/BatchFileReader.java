/*
 * The Exomiser - A tool to annotate and prioritize variants
 *
 * Copyright (C) 2012 - 2015  Charite Universitätsmedizin Berlin and Genome Research Ltd.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.charite.compbio.exomiser.cli;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;

/**
 * Reads in Exomiser batch files and returns a list of Paths to the
 * settings/analysis files. The reader expects a single path per line.
 *
 * @author Jules Jacobsen <jules.jacobsen@sanger.ac.uk>
 */
public class BatchFileReader {

    private static final Logger logger = LoggerFactory.getLogger(BatchFileReader.class);

    public List<Path> readPathsFromBatchFile(Path batchFile) {
        logger.info("Processing batch file {}", batchFile);
        try (Stream<String> lines = Files.lines(batchFile, Charset.defaultCharset())) {
            return lines.filter(commentLines()).filter(emptyLines()).map(line -> Paths.get(line.trim())).collect(toList());
        } catch (IOException ex) {
            logger.error("Unable to read batch file {}", batchFile, ex);
        }
        return new ArrayList<>();
    }

    private Predicate<String> commentLines() {
        return line -> !line.startsWith("#");
    }

    private Predicate<String> emptyLines() {
        return line -> !line.isEmpty();
    }

}
