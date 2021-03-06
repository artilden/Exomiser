/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.charite.compbio.exomiser.core.dao;

import de.charite.compbio.exomiser.core.model.VariantEvaluation;
import de.charite.compbio.exomiser.core.model.pathogenicity.CaddScore;
import de.charite.compbio.exomiser.core.model.pathogenicity.PathogenicityData;
import de.charite.compbio.exomiser.core.model.pathogenicity.PathogenicitySource;
import htsjdk.tribble.readers.TabixReader;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.VariantContextBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

/**
 *
 * @author Jules Jacobsen <jules.jacobsen@sanger.ac.uk>
 */
@RunWith(MockitoJUnitRunner.class)
public class CaddDaoTest {

    private CaddDao instance;

    @Mock
    private TabixReader snvTabixReader;
    @Mock
    private TabixReader indelTabixReader;

    private MockTabixIterator mockIterator;

    @Before
    public void setUp() {
        //What's all this about? Well we're removing the requirement for reading files from disk
        //by mocking the TabixReaders. Unfortunately these are concrete classes with no interface so
        //the mocking is event based with canned answers coming from Mockito which replicates an
        //internal call in the Dao:
        //TabixReader.Iterator results = inDelTabixReader.query(chromosome, start, start);
        //The MockTabixIterator will return a list of lines  

        mockIterator = new MockTabixIterator();
        Mockito.when(indelTabixReader.query("1:2-2")).thenReturn(mockIterator);
        Mockito.when(snvTabixReader.query("1:2-2")).thenReturn(mockIterator);
        instance = new CaddDao(indelTabixReader, snvTabixReader);
    }

    private static VariantEvaluation variant(int chr, int pos, String ref, String alt) {
        if (ref.equals("-") || alt.equals("-")) {
            //this is used to get round the fact that in real life the variant evaluation 
            //is built from a variantContext and some variantAnnotations
            return new VariantEvaluation.VariantBuilder(chr, pos, ref, alt)
                    .variantContext(Mockito.mock(VariantContext.class))
                    .build();
        }
        return new VariantEvaluation.VariantBuilder(chr, pos, ref, alt).build();
    }

    private void assertThatPathDataHasNoCaddScore(PathogenicityData result) {
        assertThat(result.getPredictedPathogenicityScores().isEmpty(), is(true));
        assertThat(result.hasPredictedScore(PathogenicitySource.CADD), is(false));
        assertThat(result.getCaddScore(), nullValue());
    }
    
    private void assertPathDataContainsCaddScore(PathogenicityData result, float score) {
        CaddScore expected = new CaddScore(score);
        assertThat(result.getPredictedPathogenicityScores().size(), equalTo(1));
        assertThat(result.hasPredictedScore(PathogenicitySource.CADD), is(true));
        assertThat(result.getCaddScore(), equalTo(expected));
    }

    @Test
    public void testGetPathogenicityData_unableToReadFromSnvSource() {
        Mockito.when(snvTabixReader.query("1:2-2")).thenThrow(IOException.class);
        assertThat(instance.getPathogenicityData(variant(1, 2, "A", "T")), equalTo(new PathogenicityData()));
    }
    
    @Test
    public void testGetPathogenicityData_snvNoData() {
        PathogenicityData result = instance.getPathogenicityData(variant(1, 2, "A", "T"));

        assertThatPathDataHasNoCaddScore(result);
    }

    @Test
    public void testGetPathogenicityData_insertionNoData() {
        PathogenicityData result = instance.getPathogenicityData(variant(1, 2, "-", "A"));

        assertThatPathDataHasNoCaddScore(result);
    }

    @Test
    public void testGetPathogenicityData_unableToReadFromInDelSource() {
        Mockito.when(indelTabixReader.query("1:2-2")).thenThrow(IOException.class);
        assertThat(instance.getPathogenicityData(variant(1, 2, "-", "A")), equalTo(new PathogenicityData()));
    }
    
    @Test
    public void testGetPathogenicityData_insertionSingleVariantAtPosition_NoMatch() {
        mockIterator.setValues(Arrays.asList(
                "1\t1\tA\tAT\t-0.234\t3.45",
                "1\t1\tA\tAC\t-0.234\t4.45"));

        PathogenicityData result = instance.getPathogenicityData(variant(1, 2, "-", "A"));

        assertThatPathDataHasNoCaddScore(result);
    }

    @Test
    public void testGetPathogenicityData_insertionSingleVariantAtPosition_OneMatch() {
        mockIterator.setValues(Arrays.asList("1\t2\tA\tAA\t-0.234\t3.45"));

        PathogenicityData result = instance.getPathogenicityData(variant(1, 2, "-", "A"));

        assertPathDataContainsCaddScore(result, 0.54814404f);
    }

    @Test
    public void testGetPathogenicityData_insertionMultipleVariantsAtPosition_OneMatch() {
        mockIterator.setValues(Arrays.asList(
                "\t\tA\tAC\t-0.234\t4.45",
                "\t\tA\tAT\t-0.234\t3.45"));

        PathogenicityData result = instance.getPathogenicityData(variant(1, 2, "-", "T"));

        assertPathDataContainsCaddScore(result, 0.54814404f);
    }

    @Test
    public void testGetPathogenicityData_deletionNoData() {
        PathogenicityData result = instance.getPathogenicityData(variant(1, 3, "A", "-"));

        assertThatPathDataHasNoCaddScore(result);
    }

    @Test
    public void testGetPathogenicityData_deletionSingleVariantAtPosition_NoMatch() {
        mockIterator.setValues(Arrays.asList(
                "1\t1\tAT\tA\t-0.234\t3.45",
                "1\t1\tAC\tA\t-0.234\t4.45"));

        PathogenicityData result = instance.getPathogenicityData(variant(1, 3, "A", "-"));

        assertThatPathDataHasNoCaddScore(result);
    }

    @Test
    public void testGetPathogenicityData_deletionSingleVariantAtPosition_OneMatch() {
        mockIterator.setValues(Arrays.asList("1\t2\tAA\tA\t-0.234\t3.45"));

        PathogenicityData result = instance.getPathogenicityData(variant(1, 3, "A", "-"));

        assertPathDataContainsCaddScore(result, 0.54814404f);
    }

    @Test
    public void testGetPathogenicityData_deletionMultipleVariantsAtPosition_OneMatch() {
        mockIterator.setValues(Arrays.asList(
                "\t\tAC\tA\t-0.234\t4.45",
                "\t\tAT\tA\t-0.234\t3.45"));

        PathogenicityData result = instance.getPathogenicityData(variant(1, 3, "T", "-"));

        assertPathDataContainsCaddScore(result, 0.54814404f);
    }

    @Test
    public void testGetPathogenicityData_snvSingleVariantAtPosition_NoMatch() {
        mockIterator.setValues(Arrays.asList(
                "1\t1\tA\tT\t-0.234\t3.45",
                "1\t1\tA\tC\t-0.234\t4.45"));

        PathogenicityData result = instance.getPathogenicityData(variant(1, 2, "A", "G"));

        assertThatPathDataHasNoCaddScore(result);
    }

    @Test
    public void testGetPathogenicityData_snvSingleVariantAtPosition_OneMatch() {
        mockIterator.setValues(Arrays.asList("1\t1\tA\tT\t-0.234\t3.45"));

        PathogenicityData result = instance.getPathogenicityData(variant(1, 2, "A", "T"));

        assertPathDataContainsCaddScore(result, 0.54814404f);
    }

    @Test
    public void testGetPathogenicityData_snvXchrSingleVariantAtPosition_OneMatch() {
        Mockito.when(snvTabixReader.query("X:1-1")).thenReturn(mockIterator);
        mockIterator.setValues(Arrays.asList("1\t1\tA\tT\t-0.234\t3.45"));

        PathogenicityData result = instance.getPathogenicityData(variant(23, 1, "A", "T"));

        assertPathDataContainsCaddScore(result, 0.54814404f);
    }

    @Test
    public void testGetPathogenicityData_snvYchrSingleVariantAtPosition_OneMatch() {
        Mockito.when(snvTabixReader.query("Y:1-1")).thenReturn(mockIterator);
        mockIterator.setValues(Arrays.asList("1\t1\tA\tT\t-0.234\t3.45"));

        PathogenicityData result = instance.getPathogenicityData(variant(24, 1, "A", "T"));

        assertPathDataContainsCaddScore(result, 0.54814404f);
    }

    @Test
    public void testGetPathogenicityData_snvMultipleVariantsAtPosition_OneMatch() {
        mockIterator.setValues(Arrays.asList(
                "\t\tA\tT\t-0.234\t3.45",
                "\t\tA\tC\t-0.234\t4.45"));

        PathogenicityData result = instance.getPathogenicityData(variant(1, 2, "A", "T"));

        assertPathDataContainsCaddScore(result, 0.54814404f);
    }

}
