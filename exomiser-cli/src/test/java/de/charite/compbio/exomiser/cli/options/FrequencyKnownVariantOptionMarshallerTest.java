/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.charite.compbio.exomiser.cli.options;

import de.charite.compbio.exomiser.core.analysis.Settings;
import de.charite.compbio.exomiser.core.prioritisers.PriorityType;
import java.nio.file.Paths;
import org.apache.commons.cli.Option;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Jules Jacobsen <jules.jacobsen@sanger.ac.uk>
 */
public class FrequencyKnownVariantOptionMarshallerTest {
    
    private FrequencyKnownVariantOptionMarshaller instance;
    private Option option;
    private Settings.SettingsBuilder settingsBuilder;
    
    @Before
    public void setUp() {
        instance = new FrequencyKnownVariantOptionMarshaller();
        option = instance.getOption();
        settingsBuilder = new Settings.SettingsBuilder();
        settingsBuilder.vcfFilePath(Paths.get("test.vcf"));
        settingsBuilder.usePrioritiser(PriorityType.OMIM_PRIORITY);
    }

    @Test
    public void testOptionCommandLineParameter() {
        assertThat(instance.getCommandLineParameter(), equalTo("remove-known-variants"));
    }
    
    @Test
    public void testThatOptionHasOptionalArgument() {
        assertThat(option.hasOptionalArg(), is(true));
    }
    
    @Test
    public void testSettingsRemoveDbSnpIsFalseByDefault() {
        Settings settings = settingsBuilder.build();        
        assertThat(settings.removeKnownVariants(), is(false));
    }

    @Test
    public void testSettingsBuilderAppliesTrueWhenSetWithNullValue() {
        instance.applyValuesToSettingsBuilder(null, settingsBuilder);
        Settings settings = settingsBuilder.build();
        
        assertThat(settings.removeKnownVariants(), is(true));
    }
    
    @Test
    public void testSettingsBuilderAppliesArgFalse() {
        String[] args = {"false"};
        instance.applyValuesToSettingsBuilder(args, settingsBuilder);
        Settings settings = settingsBuilder.build();
        
        assertThat(settings.removeKnownVariants(), is(false));
    }
    
    @Test
    public void testSettingsBuilderAppliesArgTrue() {
        String[] args = {"true"};
        instance.applyValuesToSettingsBuilder(args, settingsBuilder);
        Settings settings = settingsBuilder.build();
        
        assertThat(settings.removeKnownVariants(), is(true));
    }

}
