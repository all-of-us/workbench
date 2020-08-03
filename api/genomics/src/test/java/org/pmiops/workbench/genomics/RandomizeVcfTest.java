package org.pmiops.workbench.genomics;

import static com.google.common.truth.Truth.assertThat;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import java.io.File;
import java.util.List;
import java.util.Random;
import org.junit.Test;

public class RandomizeVcfTest {
  private static final VCFFileReader reader =
      new VCFFileReader(new File("src/test/resources/NA12878_204126160130_R01C01.toy.vcf.gz"));
  private static final Random random = new Random(0);
  private static final VariantContext variantContext = reader.iterator().next();
  private static final RandomizeVcf randomizeVcf = new RandomizeVcf(2, random);

  @Test
  public void testRandomizeVariant() {
    VariantContext randomizedVariant = randomizeVcf.randomizeVariant(variantContext);
    // For each sample/GT in the variant...
    // For each allele in the genotype...
    // ...is that allele from the bank of possible alleles at that position?
    // ignoring ref/var
    assertThat(randomizedVariant.getGenotypes().size()).isEqualTo(2);
    randomizedVariant.getGenotypes().stream()
        .flatMap(genotype -> genotype.getAlleles().stream())
        .forEach(
            allele ->
                assertThat(
                        variantContext.getAlleles().stream()
                            .anyMatch(vcAllele -> vcAllele.basesMatch(allele)))
                    .isTrue());
  }

  @Test
  public void testRandomizeGenotypes() {
    Genotype genotype = variantContext.getGenotype(0);
    Genotype randomizedGenotype = randomizeVcf.randomizeGenotype(variantContext, genotype, 0);
    assertThat(randomizedGenotype.getSampleName()).isNotEqualTo(genotype.getSampleName());
  }

  @Test
  public void testRandomizeAlleles() {
    List<Allele> alleles = randomizeVcf.randomizeAlleles(variantContext);
    assertThat(alleles.size()).isEqualTo(2); // humans ought to be diploid.
    alleles.forEach(allele -> assertThat(variantContext.getAlleles()).contains(allele));
  }
}
