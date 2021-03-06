/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.charite.compbio.exomiser.core.writers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import de.charite.compbio.exomiser.core.writers.OutputSettingsImp.OutputSettingsBuilder;
import java.util.EnumSet;
import java.util.Set;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Jules Jacobsen <jules.jacobsen@sanger.ac.uk>
 */
public class OutputSettingsImplTest {
    
    private OutputSettingsImp instance;
    private OutputSettingsBuilder builder;
    
    @Before
    public void setUp() {
        builder = new OutputSettingsBuilder();
    }

    private void buildInstance() {
        instance = builder.build();
    }
    
    @Test
    public void testThatDefaultOutputPassVariantsOptionIsFalse() {
        buildInstance();
        assertThat(instance.outputPassVariantsOnly(), equalTo(false));
    }
    
    @Test
    public void testThatBuilderProducesOutputPassVariantsOptionWhenSet() {
        builder.outputPassVariantsOnly(true);
        buildInstance();
        assertThat(instance.outputPassVariantsOnly(), equalTo(true));
    }

    /**
     * Test of getNumberOfGenesToShow method, of class ExomiserSettings.
     */
    @Test
    public void testThatDefaultNumberOfGenesToShowIsZero() {
        buildInstance();
        assertThat(instance.getNumberOfGenesToShow(), equalTo(0));
    }
    
    @Test
    public void testThatBuilderCanSetNumberOfGenesToShow() {
        int numGenes = 200;
        builder.numberOfGenesToShow(numGenes);
        buildInstance();
        assertThat(instance.getNumberOfGenesToShow(), equalTo(numGenes));
    }

    /**
     * Test of getOutputPrefix method, of class ExomiserSettings.
     */
    @Test
    public void testThatBuilderProducesDefaultOutFileName() {
        buildInstance();
        assertThat(instance.getOutputPrefix(), equalTo(""));
    }
    
    @Test
    public void testThatBuilderProducesSetOutFileName() {
        String outputPrefix = "wibble";
        builder.outputPrefix(outputPrefix);
        buildInstance();
        assertThat(instance.getOutputPrefix(), equalTo(outputPrefix));
    }

    /**
     * Test of getOutputFormats method, of class ExomiserSettings.
     */
    @Test
    public void testThatDefaultOutputFormatIsHtml() {
        buildInstance();
        assertThat(instance.getOutputFormats(), equalTo((Set<OutputFormat>) EnumSet.of(OutputFormat.HTML)));
    }

    @Test
    public void testThatBuilderProducesSetOutputFormat() {
        Set<OutputFormat> outputFormats = EnumSet.of(OutputFormat.TSV_GENE);
        builder.outputFormats(outputFormats);
        buildInstance();
        assertThat(instance.getOutputFormats(), equalTo(outputFormats));
    }
    
    @Test
    public void testHashCode() {
        buildInstance();
        OutputSettingsImp other = new OutputSettingsBuilder().build();
        assertThat(instance.hashCode(), equalTo(other.hashCode()));
    }
    
    @Test
    public void testEquals() {
        buildInstance();
        OutputSettingsImp other = new OutputSettingsBuilder().build();
        assertThat(instance, equalTo(other));
    }
    
    @Test
    public void testToString() {
        buildInstance();
        System.out.println(instance);
        assertThat(instance.toString().isEmpty(), is(false));
    }
    
    @Test
    public void testCanBuildFromYaml() throws Exception {
        buildInstance();
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        
        OutputSettings createdFromYaml = mapper.readValue("outputPassVariantsOnly: false\n"
                + "numGenes: 0\n"
                + "outputPrefix: \n"
                + "outputFormats: [HTML]",
                OutputSettingsImp.class);
        
        assertThat(instance, equalTo(createdFromYaml));   
    }
    
    @Test
    public void testCanOutputAsYaml() throws Exception {
        buildInstance();
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        String output = mapper.writeValueAsString(instance);
        System.out.println(output);        
    }
}
