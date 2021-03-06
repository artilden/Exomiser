/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.charite.compbio.exomiser.core.model.pathogenicity;

/**
 *
 * @author Jules Jacobsen <jules.jacobsen@sanger.ac.uk>
 */
public class MutationTasterScore extends BasePathogenicityScore {
    
    public static final float MTASTER_THRESHOLD = 0.94f;

    public MutationTasterScore(float score) {
        super(score, PathogenicitySource.MUTATION_TASTER);
    }

    @Override
    public String toString() {
        if (score > MTASTER_THRESHOLD) {
            return String.format("Mutation Taster: %.3f (P)", score);
        } else {
            return String.format("Mutation Taster: %.3f", score);
        }
    }

    
}
