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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.charite.compbio.exomiser.core.factories;

import de.charite.compbio.jannovar.annotation.VariantEffect;
import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.htsjdk.VariantContextAnnotator;
import de.charite.compbio.jannovar.pedigree.Genotype;
import de.charite.compbio.jannovar.reference.HG19RefDictBuilder;
import de.charite.compbio.jannovar.reference.TranscriptModel;
import htsjdk.variant.variantcontext.VariantContext;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import de.charite.compbio.jannovar.annotation.Annotation;
import de.charite.compbio.jannovar.annotation.VariantAnnotations;
import static org.hamcrest.CoreMatchers.is;

/**
 *
 * @author Jules Jacobsen <jules.jacobsen@sanger.ac.uk>
 */
public class VariantAnnotatorTest {

    private final TestVariantFactory varFactory = new TestVariantFactory();
    private final TranscriptModel tmFGFR2 = TestTranscriptModelFactory.buildTMForFGFR2();
    private final TranscriptModel tmSHH = TestTranscriptModelFactory.buildTMForSHH();
    private final JannovarData jannovarData = TestFactory.buildJannovarData(tmFGFR2, tmSHH);

    private VariantAnnotator instance = new VariantAnnotator(jannovarData);

    @Test
    public void alternateConstructor() {
        VariantContextAnnotator variantContextAnnotator = new VariantContextAnnotator(jannovarData.getRefDict(), jannovarData.getChromosomes());
        VariantAnnotator alternateConstructorInstance = new VariantAnnotator(variantContextAnnotator);
        assertThat(alternateConstructorInstance, notNullValue());
    }

    @Test(expected = NullPointerException.class)
    public void testAnnotationOfNullThrowsNullPointer() throws Exception {
        instance.buildVariantAnnotations(null);
    }

    @Test
    public void testIntergenicAnnotationNoNeighbour() {
        // intergenic variants should have empty annotation lists if there is no neighbouring gene
        VariantContext vc = varFactory.constructVariantContext(2, 2, "A", "T", Genotype.HETEROZYGOUS, 30);

        List<VariantAnnotations> vars = instance.buildVariantAnnotations(vc);
        VariantAnnotations variant = vars.get(0);
        List<Annotation> annotationList = variant.getAnnotations();
        //these are equivalent assertions
        assertThat(vars.size(), equalTo(1));
        assertThat(variant.hasAnnotation(), is(false));
        assertThat(annotationList.isEmpty(), is(true));
    }

    @Test
    public void testIntergenicAnnotationWithNeighbour() {
        // intergenic variants should have an entry for neighbouring genes
        VariantContext vc = varFactory.constructVariantContext(10, 2, "A", "T", Genotype.HETEROZYGOUS, 30);

        List<VariantAnnotations> vars = instance.buildVariantAnnotations(vc);
        VariantAnnotations variant = vars.get(0);
        List<Annotation> annotationList = variant.getAnnotations();
        Annotation annotation = annotationList.get(0);

        assertThat(vars.size(), equalTo(1));
        assertThat(annotationList.size(), equalTo(1));
        assertThat(annotation.getEffects(), equalTo(ImmutableSortedSet.of(VariantEffect.INTERGENIC_VARIANT)));
        assertThat(annotation.getTranscript(), equalTo(tmFGFR2));
    }

    @Test
    public void testExonicAnnotationInFGFR2Gene() {
        // a variant in the FGFR2 gene should be annotated correctly
        VariantContext vc = varFactory.constructVariantContext(10, 123353320, "A", "T", Genotype.HETEROZYGOUS, 30);

        List<VariantAnnotations> vars = instance.buildVariantAnnotations(vc);
        VariantAnnotations variant = vars.get(0);
        List<Annotation> annotationList = variant.getAnnotations();
        Annotation annotation = annotationList.get(0);

        assertThat(vars.size(), equalTo(1));
        assertThat(annotationList.size(), equalTo(1));
        assertThat(annotation.getEffects(), equalTo(ImmutableSortedSet.of(VariantEffect.STOP_GAINED)));
        assertThat(annotation.getTranscript(), equalTo(tmFGFR2));
    }

    @Test
    public void testExonicAnnotationInSHHGene() {
        // a variant in the SHH gene should be annotated correctly
        VariantContext vc = varFactory.constructVariantContext(7, 155604810, "A", "T", Genotype.HETEROZYGOUS, 30);

        List<VariantAnnotations> vars = instance.buildVariantAnnotations(vc);
        VariantAnnotations variant = vars.get(0);
        List<Annotation> annotationList = variant.getAnnotations();
        Annotation annotation = annotationList.get(0);

        assertThat(vars.size(), equalTo(1));
        assertThat(annotationList.size(), equalTo(1));
        assertThat(annotation.getEffects(), equalTo(ImmutableSortedSet.of(VariantEffect.SYNONYMOUS_VARIANT)));
        assertThat(annotation.getTranscript(), equalTo(tmSHH));
    }

}
