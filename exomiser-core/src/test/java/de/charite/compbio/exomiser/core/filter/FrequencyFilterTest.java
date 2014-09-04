package de.charite.compbio.exomiser.core.filter;

import de.charite.compbio.exomiser.core.filter.FrequencyFilter;
import de.charite.compbio.exomiser.core.filter.FilterType;
import de.charite.compbio.exomiser.core.frequency.Frequency;
import de.charite.compbio.exomiser.core.frequency.FrequencyData;
import de.charite.compbio.exomiser.core.model.VariantEvaluation;
import jannovar.exome.Variant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FrequencyFilterTest {
    
    FrequencyFilter instance;
    
    private VariantEvaluation passesEspAllFrequency;
    private VariantEvaluation passesEspAAFrequency;
    private VariantEvaluation passesEspEAFrequency;
    private VariantEvaluation passesDbsnpFrequency;
    
    private VariantEvaluation failsFrequency;
    private VariantEvaluation passesNoFrequencyData;
    
    @Mock
    Variant mockVariant;
    
    private static final float FREQ_THRESHOLD = 0.1f;
    private static final float PASS_FREQ = FREQ_THRESHOLD - 0.02f;
    private static final float FAIL_FREQ = FREQ_THRESHOLD + 1.0f;
    
    private static final Frequency ESP_ALL_PASS = new Frequency(PASS_FREQ);
    private static final Frequency ESP_ALL_FAIL = new Frequency(FAIL_FREQ);
    
    private static final Frequency ESP_AA_PASS = new Frequency(PASS_FREQ);
    private static final Frequency ESP_AA_FAIL = new Frequency(FAIL_FREQ);
    
    private static final Frequency ESP_EA_PASS = new Frequency(PASS_FREQ);
    private static final Frequency ESP_EA_FAIL = new Frequency(FAIL_FREQ);
    
    private static final Frequency DBSNP_PASS = new Frequency(PASS_FREQ);
    private static final Frequency DBSNP_FAIL = new Frequency(FAIL_FREQ);    
    
    
    private static final FrequencyData espAllPassData = new FrequencyData(null, null, ESP_ALL_PASS, null, null);
    private static final FrequencyData espAllFailData = new FrequencyData(null, null, ESP_ALL_FAIL, null, null);
    private static final FrequencyData espAaPassData = new FrequencyData(null, null, null, ESP_AA_PASS, null);
    private static final FrequencyData espEaPassData = new FrequencyData(null, null, null, null, ESP_EA_PASS);
    private static final FrequencyData dbSnpPassData = new FrequencyData(null, DBSNP_PASS, null, null, null);
    private static final FrequencyData noFreqData = new FrequencyData(null, null, null, null, null);

    @Before
    public void setUp() throws Exception {
        
        MockitoAnnotations.initMocks(this);
        
        instance = new FrequencyFilter(FREQ_THRESHOLD, false);
        
        passesEspAllFrequency = new VariantEvaluation(mockVariant);
        passesEspAllFrequency.setFrequencyData(espAllPassData);
        
        passesEspAAFrequency = new VariantEvaluation(mockVariant);
        passesEspAAFrequency.setFrequencyData(espAaPassData);
        
        passesEspEAFrequency = new VariantEvaluation(mockVariant);
        passesEspEAFrequency.setFrequencyData(espEaPassData);
        
        passesDbsnpFrequency = new VariantEvaluation(mockVariant);
        passesDbsnpFrequency.setFrequencyData(dbSnpPassData);
        
        failsFrequency = new VariantEvaluation(mockVariant);
        failsFrequency.setFrequencyData(espAllFailData);
                
        passesNoFrequencyData = new VariantEvaluation(mockVariant);
        passesNoFrequencyData.setFrequencyData(noFreqData);
    }
    
    @Test
    public void testGetFilterType() {
        assertThat(instance.getFilterType(), equalTo(FilterType.FREQUENCY_FILTER));
    }
    
    @Test
    public void testFilterVariantsByFrequencyThreshold() {
        boolean filterOutAllKnownVariants = false;
        
        instance = new FrequencyFilter(FREQ_THRESHOLD, filterOutAllKnownVariants);
        
        List<VariantEvaluation> variantList = new ArrayList<>();
        variantList.add(passesDbsnpFrequency);
        variantList.add(passesEspAAFrequency);
        variantList.add(passesEspAllFrequency);
        variantList.add(passesEspEAFrequency);
        variantList.add(passesNoFrequencyData);
        variantList.add(failsFrequency);
        
        instance.filterVariants(variantList);
        
        Set failedFilterSet = EnumSet.of(FilterType.FREQUENCY_FILTER);
        
        assertThat(failsFrequency.passesFilters(), is(false));
        assertThat(failsFrequency.getFailedFilters(), equalTo(failedFilterSet));
        
        assertThat(passesDbsnpFrequency.passesFilters(), is(true));
        assertThat(passesDbsnpFrequency.getFailedFilters().isEmpty(), is(true));
        
        assertThat(passesEspAAFrequency.passesFilters(), is(true));
        assertThat(passesEspAAFrequency.getFailedFilters().isEmpty(), is(true));
        
        assertThat(passesEspAllFrequency.passesFilters(), is(true));
        assertThat(passesEspAllFrequency.getFailedFilters().isEmpty(), is(true));
        
        assertThat(passesEspEAFrequency.passesFilters(), is(true));
        assertThat(passesEspEAFrequency.getFailedFilters().isEmpty(), is(true));
        
        assertThat(passesNoFrequencyData.passesFilters(), is(true));
        assertThat(passesNoFrequencyData.getFailedFilters().isEmpty(), is(true));
           
    }
    
    @Test
    public void testFilterOutAllKnownVariants() {
        boolean filterOutAllKnownVariants = true;
        
        instance = new FrequencyFilter(FREQ_THRESHOLD, filterOutAllKnownVariants);
        
        List<VariantEvaluation> variantList = new ArrayList<>();
        variantList.add(passesDbsnpFrequency);
        variantList.add(passesEspAAFrequency);
        variantList.add(passesEspAllFrequency);
        variantList.add(passesEspEAFrequency);
        variantList.add(passesNoFrequencyData);
        variantList.add(failsFrequency);
        
        instance.filterVariants(variantList);
        
        Set failedFilterSet = EnumSet.of(FilterType.FREQUENCY_FILTER);
        
        assertThat(failsFrequency.passesFilters(), is(false));
        assertThat(failsFrequency.getFailedFilters(), equalTo(failedFilterSet));
        
        assertThat(passesDbsnpFrequency.passesFilters(), is(false));
        assertThat(passesDbsnpFrequency.getFailedFilters(), equalTo(failedFilterSet));
        
        assertThat(passesEspAAFrequency.passesFilters(), is(false));
        assertThat(passesEspAAFrequency.getFailedFilters(), equalTo(failedFilterSet));
        
        assertThat(passesEspAllFrequency.passesFilters(), is(false));
        assertThat(passesEspAllFrequency.getFailedFilters(), equalTo(failedFilterSet));
        
        assertThat(passesEspEAFrequency.passesFilters(), is(false));
        assertThat(passesEspEAFrequency.getFailedFilters(), equalTo(failedFilterSet));
        
        assertThat(passesNoFrequencyData.passesFilters(), is(true));
        assertThat(passesNoFrequencyData.getFailedFilters().isEmpty(), is(true));
              
    }
     
    @Test
    public void testPassesFilterFails() {
        assertThat(instance.passesFilter(espAllFailData), is(false));
    }
    
    @Test
    public void testEaspAllPassesFilter() {
        assertThat(instance.passesFilter(espAllPassData), is(true));
    }
    
    @Test
    public void testNoFrequencyDataPassesFilter() {
        assertThat(instance.passesFilter(noFreqData), is(true));
    }
    
    @Test
    public void testCalculateScore() {
//        fail("Not yet implemented");
    }
    
    @Test
    public void testMakeReport() {
//        fail("Not yet implemented");
    }
}