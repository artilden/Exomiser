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

package de.charite.compbio.exomiser.core.analysis;

import de.charite.compbio.exomiser.core.factories.VariantDataService;
import de.charite.compbio.exomiser.core.filters.*;
import de.charite.compbio.exomiser.core.model.GeneticInterval;
import de.charite.compbio.exomiser.core.model.frequency.FrequencySource;
import de.charite.compbio.exomiser.core.model.pathogenicity.PathogenicitySource;
import de.charite.compbio.exomiser.core.prioritisers.ExomeWalkerPriority;
import de.charite.compbio.exomiser.core.prioritisers.HiPhiveOptions;
import de.charite.compbio.exomiser.core.prioritisers.HiPhivePriority;
import de.charite.compbio.exomiser.core.prioritisers.PriorityFactory;
import de.charite.compbio.exomiser.core.prioritisers.PriorityType;
import de.charite.compbio.exomiser.core.prioritisers.ScoringMode;
import de.charite.compbio.exomiser.core.writers.OutputFormat;
import de.charite.compbio.exomiser.core.writers.OutputSettings;
import de.charite.compbio.exomiser.core.writers.OutputSettingsImp.OutputSettingsBuilder;
import de.charite.compbio.jannovar.annotation.VariantEffect;
import de.charite.compbio.jannovar.pedigree.ModeOfInheritance;
import de.charite.compbio.jannovar.reference.HG19RefDictBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;

import static java.nio.file.Files.newInputStream;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

/**
 * @since 7.0.0
 * @author Jules Jacobsen <jules.jacobsen@sanger.ac.uk>
 */
public class AnalysisParser {

    private static final Logger logger = LoggerFactory.getLogger(AnalysisParser.class);

    private final PriorityFactory prioritiserFactory;
    private final VariantDataService variantDataService;

    public AnalysisParser(PriorityFactory prioritiserFactory, VariantDataService variantDataService) {
        this.prioritiserFactory = prioritiserFactory;
        this.variantDataService = variantDataService;
    }

    public Analysis parseAnalysis(Path analysisScript) {
        Yaml yaml = new Yaml();
        Map settingsMap = (Map) yaml.load(readPath(analysisScript));
        return constructAnalysisFromMap(settingsMap);
    }

    public Analysis parseAnalysis(String analysisDoc) {
        Yaml yaml = new Yaml();
        Map settingsMap = (Map) yaml.load(analysisDoc);
        return constructAnalysisFromMap(settingsMap);
    }

    public OutputSettings parseOutputSettings(Path analysisScript) {
        Yaml yaml = new Yaml();
        Map settingsMap = (Map) yaml.load(readPath(analysisScript));
        return constructOutputSettingsFromMap(settingsMap);
    }

    public OutputSettings parseOutputSettings(String analysisDoc) {
        Yaml yaml = new Yaml();
        Map settingsMap = (Map) yaml.load(analysisDoc);
        return constructOutputSettingsFromMap(settingsMap);
    }

    private Analysis constructAnalysisFromMap(Map settingsMap) {
        AnalysisConstructor analysisConstructor = new AnalysisConstructor();
        Analysis analysis = analysisConstructor.construct((Map) settingsMap.get("analysis"));
        return analysis;
    }

    private OutputSettings constructOutputSettingsFromMap(Map settingsMap) {
        OutputSettingsConstructor outputSettingsConstructor = new OutputSettingsConstructor();
        OutputSettings outputSettings = outputSettingsConstructor.construct((Map) settingsMap.get("outputOptions"));
        return outputSettings;
    }

    private BufferedReader readPath(Path analysisDoc) {
        try {
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
            Reader reader = new InputStreamReader(newInputStream(analysisDoc), decoder);
            return new BufferedReader(reader);
        } catch (IOException ex) {
            throw new AnalysisFileNotFoundException("Unable to find analysis file: " + ex.getMessage());
        }
    }

    protected static class AnalysisFileNotFoundException extends RuntimeException {

        AnalysisFileNotFoundException(String message) {
            super(message);
        }
    }

    private class OutputSettingsConstructor {

        public OutputSettings construct(Map analysisMap) {
            OutputSettingsBuilder builder = new OutputSettingsBuilder();
            builder.outputPassVariantsOnly(parseOutputPassVariantsOnly(analysisMap));
            builder.numberOfGenesToShow(parseNumberOfGenesToShow(analysisMap));
            builder.outputPrefix(parseOutputPrefix(analysisMap));
            builder.outputFormats(parseOutputFormats(analysisMap));
            return builder.build();
        }

        private Boolean parseOutputPassVariantsOnly(Map<String, Boolean> analysisMap) {
            Boolean outputPassOnly = analysisMap.get("outputPassVariantsOnly");
            if (outputPassOnly == null) {
                throw new AnalysisParserException("outputPassVariantsOnly cannot be null.", analysisMap);
            }
            return outputPassOnly;
        }

        private int parseNumberOfGenesToShow(Map<String, Integer> analysisMap) {
            Integer genesToShow = analysisMap.get("numGenes");
            if (genesToShow == null) {
                genesToShow = 0;
            }
            return genesToShow;
        }

        private String parseOutputPrefix(Map<String, String> analysisMap) {
            String outputPrefix = analysisMap.get("outputPrefix");
            if (outputPrefix == null) {
                outputPrefix = "exomiser-results";
            }
            return outputPrefix;
        }

        private Set<OutputFormat> parseOutputFormats(Map<String, List<String>> analysisMap) {
            List<String> givenOutputFormats = analysisMap.get("outputFormats");
            if (givenOutputFormats == null) {
                givenOutputFormats = new ArrayList<>();
            }
            Set<OutputFormat> parsedOutputFormats = new LinkedHashSet<>();
            for (String name : givenOutputFormats) {
                switch (name.trim()) {
                    case "HTML":
                        parsedOutputFormats.add(OutputFormat.HTML);
                        break;
                    case "TSV_GENE":
                    case "TAB-GENE":
                    case "TSV-GENE":
                        parsedOutputFormats.add(OutputFormat.TSV_GENE);
                        break;
                    case "TSV_VARIANT":
                    case "TAB-VARIANT":
                    case "TSV-VARIANT":
                        parsedOutputFormats.add(OutputFormat.TSV_VARIANT);
                        break;
                    case "VCF":
                        parsedOutputFormats.add(OutputFormat.VCF);
                        break;
                    case "PHENOGRID":
                        parsedOutputFormats.add(OutputFormat.PHENOGRID);
                        break;
                    default:
                        logger.info("{} is not a recognised output format. Please choose one or more of HTML, TAB-GENE, TAB-VARIANT, VCF - defaulting to HTML", name);
                        parsedOutputFormats.add(OutputFormat.HTML);
                        break;
                }
            }
            return EnumSet.copyOf(parsedOutputFormats);
        }
    }

    private class AnalysisConstructor {

        public Analysis construct(Map analysisMap) {

            Analysis analysis = new Analysis();
            analysis.setVcfPath(parseVcf(analysisMap));
            analysis.setPedPath(parsePed(analysisMap));
            analysis.setHpoIds(parseHpoIds(analysisMap));
            analysis.setModeOfInheritance(parseModeOfInheritance(analysisMap));
            analysis.setScoringMode(parseScoringMode(analysisMap));
            analysis.setAnalysisMode(parseAnalysisMode(analysisMap));
            analysis.setFrequencySources(parseFrequencySources(analysisMap));
            analysis.setPathogenicitySources(parsePathogenicitySources(analysisMap));
            analysis.addAllSteps(makeAnalysisSteps(analysisMap));

            logger.debug("Made analysis: {}", analysis);
            return analysis;
        }

        private List<AnalysisStep> makeAnalysisSteps(Map analysisMap) {
            List<AnalysisStep> analysisSteps = new ArrayList<>();
            for (Map<String, Map> analysisStepMap : parseAnalysisSteps(analysisMap)) {
                logger.debug("Analysis step: {}", analysisStepMap);
                for (Entry<String, Map> entry : analysisStepMap.entrySet()) {
                    AnalysisStep analysisStep = makeAnalysisStep(entry, analysisMap);
                    if (analysisStep != null) {
                        analysisSteps.add(analysisStep);
                        logger.debug("Added {}", entry.getKey());
                    }
                }
            }
            return analysisSteps;
        }

        private Path parseVcf(Map<String, String> analysisMap) {
            String vcfValue = analysisMap.get("vcf");
            //VCF file paths are not allowed to be null
            if (vcfValue == null) {
                throw new AnalysisParserException("VCF path cannot be null.", analysisMap);
            }
            return Paths.get(vcfValue);
        }

        private Path parsePed(Map<String, String> analysisMap) {
            String pedValue = analysisMap.get("ped");
            //PED file paths are allowed to be null
            if (pedValue == null) {
                return null;
            }
            return Paths.get(pedValue);
        }

        private ModeOfInheritance parseModeOfInheritance(Map<String, String> analysisMap) {
            String value = analysisMap.get("modeOfInheritance");
            if (value == null) {
                return ModeOfInheritance.UNINITIALIZED;
            }
            if (value.equals("UNDEFINED")) {
                return ModeOfInheritance.UNINITIALIZED;
            }
            return ModeOfInheritance.valueOf(value);
        }

        private AnalysisMode parseAnalysisMode(Map<String, String> analysisMap) {
            String value = analysisMap.get("analysisMode");
            if (value == null) {
                return AnalysisMode.PASS_ONLY;
            }
            return AnalysisMode.valueOf(value);
        }

        private ScoringMode parseScoringMode(Map<String, String> analysisMap) {
            String value = analysisMap.get("geneScoreMode");
            if (value == null) {
                return ScoringMode.RAW_SCORE;
            }
            return ScoringMode.valueOf(value);
        }

        private List<String> parseHpoIds(Map<String, List> analysisMap) {
            List<String> hpoIds = analysisMap.get("hpoIds");
            if (hpoIds == null) {
                hpoIds = new ArrayList();
            }
            return hpoIds;
        }

        private List<Map<String, Map>> parseAnalysisSteps(Map analysisMap) {
            List steps = (List) analysisMap.get("steps");
            if (steps == null) {
                steps = new ArrayList();
            }
            return steps;
        }

        /**
         * Returns an AnalysisStep or null if the step is unrecognised.
         *
         * @return
         */
        private AnalysisStep makeAnalysisStep(Entry<String, Map> entry, Map analysisMap) {
            String key = entry.getKey();
            Map analysisStepMap = entry.getValue();
            switch (key) {
                case "intervalFilter":
                    return makeIntervalFilter(analysisStepMap);
                case "geneIdFilter":
                    return makeGeneIdFilter(analysisStepMap);
                case "variantEffectFilter":
                    return makeVariantEffectFilter(analysisStepMap);
                case "qualityFilter":
                    return makeQualityFilter(analysisStepMap);
                case "knownVariantFilter":
                    return makeKnownVariantFilter(analysisStepMap, parseFrequencySources(analysisMap));
                case "frequencyFilter":
                    return makeFrequencyFilter(analysisStepMap, parseFrequencySources(analysisMap));
                case "pathogenicityFilter":
                    return makePathogenicityFilter(analysisStepMap, parsePathogenicitySources(analysisMap));
                case "inheritanceFilter":
                    return makeInheritanceFilter(parseModeOfInheritance(analysisMap));
                case "priorityScoreFilter":
                    return makePriorityScoreFilter(analysisStepMap);
                case "regulatoryFeatureFilter":
                    return makeRegulatoryFeatureFilter(analysisStepMap);
                case "omimPrioritiser":
                    return prioritiserFactory.makeOmimPrioritiser();
                case "hiPhivePrioritiser":
                    return makeHiPhivePrioritiser(analysisStepMap, parseHpoIds(analysisMap));
                case "phivePrioritiser":
                    return prioritiserFactory.makePhivePrioritiser(parseHpoIds(analysisMap));
                case "phenixPrioritiser":
                    return prioritiserFactory.makePhenixPrioritiser(parseHpoIds(analysisMap));
                case "exomeWalkerPrioritiser":
                    return makeWalkerPrioritiser(analysisStepMap);
                default:
                    //throw exception here?
                    logger.error("Unsupported exomiser step: {}", key);
                    return null;
            }
        }

        private IntervalFilter makeIntervalFilter(Map<String, String> options) {
            String interval = options.get("interval");
            if (interval == null) {
                throw new AnalysisParserException("Interval filter requires a valid genetic interval e.g. {interval: 'chr10:122892600-122892700'}", options);
            }
            return new IntervalFilter(GeneticInterval.parseString(HG19RefDictBuilder.build(), interval));
        }

        private EntrezGeneIdFilter makeGeneIdFilter(Map<String, List> options) {
            List<Integer> geneIds = options.get("geneIds");
            if (geneIds == null || geneIds.isEmpty()) {
                throw new AnalysisParserException("GeneId filter requires a list of ENTREZ geneIds e.g. {geneIds: [12345, 34567, 98765]}", options);
            }
            return new EntrezGeneIdFilter(new LinkedHashSet<>(geneIds));
        }

        private VariantEffectFilter makeVariantEffectFilter(Map<String, List> options) {
            List<String> effectsToRemove = options.get("remove");
            if (effectsToRemove == null) {
                throw new AnalysisParserException("VariantEffect filter requires a list of VariantEffects to be removed e.g. {remove: [UPSTREAM_GENE_VARIANT, INTERGENIC_VARIANT, SYNONYMOUS_VARIANT]}", options);
            }
            List<VariantEffect> variantEffects = new ArrayList<>();
            for (String effect : effectsToRemove) {
                try {
                    VariantEffect variantEffect = VariantEffect.valueOf(effect);
                    variantEffects.add(variantEffect);
                } catch (IllegalArgumentException ex) {
                    throw new AnalysisParserException(String.format("Illegal VariantEffect: '%s'.%nPermitted effects are any of: %s.", effect, EnumSet.allOf(VariantEffect.class)), options);
                }
            }
            return new VariantEffectFilter(EnumSet.copyOf(variantEffects));
        }

        private QualityFilter makeQualityFilter(Map<String, Double> options) {
            Double quality = options.get("minQuality");
            if (quality == null) {
                throw new AnalysisParserException("Quality filter requires a floating point value for the minimum PHRED score e.g. {minQuality: 50.0}", options);
            }
            return new QualityFilter(quality);
        }

        private VariantFilter makeKnownVariantFilter(Map<String, Object> options, Set<FrequencySource> sources) {
            //nothing special to do here, apart from wrap the filter with a DataProvider, this is a boolean filter.
            if (sources.isEmpty()) {
                throw new AnalysisParserException("Known variant filter requires a list of frequency sources for the analysis e.g. frequencySources: [THOUSAND_GENOMES, ESP_ALL]", options);
            }
            return new FrequencyDataProvider(variantDataService, EnumSet.copyOf(sources), new KnownVariantFilter());
        }

        private VariantFilter makeFrequencyFilter(Map<String, Object> options, Set<FrequencySource> sources) {
            Double maxFreq = getMaxFrequency(options);
            if (sources.isEmpty()) {
                throw new AnalysisParserException("Frequency filter requires a list of frequency sources for the analysis e.g. frequencySources: [THOUSAND_GENOMES, ESP_ALL]", options);
            }
            return new FrequencyDataProvider(variantDataService, EnumSet.copyOf(sources), new FrequencyFilter(maxFreq.floatValue()));
        }

        private Double getMaxFrequency(Map<String, Object> options) {
            Double maxFreq = (Double) options.get("maxFrequency");
            if (maxFreq == null) {
                throw new AnalysisParserException("Frequency filter requires a floating point value for the maximum frequency e.g. {maxFrequency: 1.0}", options);
            }
            return maxFreq;
        }
        
        private Set<FrequencySource> parseFrequencySources(Map<String, Object> options) {
            List<String> frequencySources = (List<String>) options.get("frequencySources");
            if (frequencySources == null) {
                return EnumSet.noneOf(FrequencySource.class);
            }
            List<FrequencySource> sources = new ArrayList<>();
            for (String source : frequencySources) {
                try {
                    FrequencySource frequencySource = FrequencySource.valueOf(source);
                    sources.add(frequencySource);
                } catch (IllegalArgumentException ex) {
                    throw new AnalysisParserException(String.format("Illegal FrequencySource: '%s'.%nPermitted sources are any of: %s.", source, EnumSet.allOf(FrequencySource.class)), options);
                }
            }
            if (sources.isEmpty()) {
                return EnumSet.noneOf(FrequencySource.class);
            }
            return EnumSet.copyOf(sources);
        }

        private VariantFilter makePathogenicityFilter(Map<String, Object> options, Set<PathogenicitySource> sources) {
            Boolean keepNonPathogenic = (Boolean) options.get("keepNonPathogenic");
            if (keepNonPathogenic == null) {
                throw new AnalysisParserException("Pathogenicity filter requires a boolean value for keepNonPathogenic e.g. {keepNonPathogenic: false}", options);
            }
            if (sources.isEmpty()) {
                throw new AnalysisParserException("Pathogenicity filter requires a list of pathogenicity sources for the analysis e.g. {pathogenicitySources: [SIFT, POLYPHEN, MUTATION_TASTER]}", options);
            }
            return new PathogenicityDataProvider(variantDataService, EnumSet.copyOf(sources), new PathogenicityFilter(keepNonPathogenic));
        }

        private Set<PathogenicitySource> parsePathogenicitySources(Map<String, Object> options) {
            List<String> pathogenicitySources = (List<String>) options.get("pathogenicitySources");
            if (pathogenicitySources == null) {
                return EnumSet.noneOf(PathogenicitySource.class);
            }
            List<PathogenicitySource> sources = new ArrayList<>();
            for (String source : pathogenicitySources) {
                try {
                    PathogenicitySource pathogenicitySource = PathogenicitySource.valueOf(source);
                    sources.add(pathogenicitySource);
                } catch (IllegalArgumentException ex) {
                    throw new AnalysisParserException(String.format("Illegal PathogenicitySource: '%s'.%nPermitted sources are any of: %s.", source, EnumSet.allOf(PathogenicitySource.class)), options);
                }
            }
            if (sources.isEmpty()) {
                return EnumSet.noneOf(PathogenicitySource.class);
            }
            return EnumSet.copyOf(sources);
        }

        private PriorityScoreFilter makePriorityScoreFilter(Map<String, Object> options) {
            String priorityTypeString = (String) options.get("priorityType");
            if (priorityTypeString == null) {
                throw new AnalysisParserException("Priority score filter requires a string value for the prioritiser type e.g. {priorityType: HIPHIVE_PRIORITY}", options);
            }
            PriorityType priorityType = PriorityType.valueOf(priorityTypeString);

            Double minPriorityScore = (Double) options.get("minPriorityScore");
            if (minPriorityScore == null) {
                throw new AnalysisParserException("Priority score filter requires a floating point value for the minimum prioritiser score e.g. {minPriorityScore: 0.65}", options);
            }
            return new PriorityScoreFilter(priorityType, minPriorityScore.floatValue());
        }

        private VariantFilter makeRegulatoryFeatureFilter(Map<String, Double> options) {
            return new RegulatoryFeatureFilter();
        }

        private InheritanceFilter makeInheritanceFilter(ModeOfInheritance modeOfInheritance) {
            if (modeOfInheritance == ModeOfInheritance.UNINITIALIZED) {
                logger.info("Not making an inheritance filter for {} mode of inheritance", modeOfInheritance);
                return null;
            }
            return new InheritanceFilter(modeOfInheritance);
        }

        private HiPhivePriority makeHiPhivePrioritiser(Map<String, String> options, List<String> hpoIds) {
            HiPhiveOptions hiPhiveOptions = new HiPhiveOptions();
            if (!options.isEmpty()) {
                String diseaseId = options.get("diseaseId");
                if (diseaseId == null) {
                    diseaseId = "";
                }
                String candidateGeneSymbol = options.get("candidateGeneSymbol");
                if (candidateGeneSymbol == null) {
                    candidateGeneSymbol = "";
                }
                String runParams = options.get("runParams");
                if (runParams == null) {
                    runParams = "";
                }
                hiPhiveOptions = new HiPhiveOptions(diseaseId, candidateGeneSymbol, runParams);
                logger.info("Made {}", hiPhiveOptions);
            }
            return prioritiserFactory.makeHiPhivePrioritiser(hpoIds, hiPhiveOptions);
        }

        private ExomeWalkerPriority makeWalkerPrioritiser(Map<String, List> options) {
            List geneIds = options.get("seedGeneIds");
            if (geneIds == null || geneIds.isEmpty()) {
                throw new AnalysisParserException("ExomeWalker prioritiser requires a list of ENTREZ geneIds e.g. {seedGeneIds: [11111, 22222, 33333]}", options);
            }
            return prioritiserFactory.makeExomeWalkerPrioritiser(geneIds);
        }

    }

    protected static class AnalysisParserException extends RuntimeException {

        AnalysisParserException(String message, Map options) {
            super(message + " was " + new Yaml().dump(options));
        }
    }

}
