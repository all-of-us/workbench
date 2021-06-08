package org.pmiops.workbench.genomics;

import static com.google.common.truth.Truth.assertThat;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

public class RandomizeVcfTest {
  private static final VCFFileReader reader =
      new VCFFileReader(new File("src/test/resources/NA12878_204126160130_R01C01.toy.vcf.gz"));
  private static final Random random = new Random(0);
  private static final VariantContext variantContext = reader.iterator().next();

  private static final List<String> sampleNames = Arrays.asList("sn1", "sn2");
  private static final RandomizeVcf randomizeVcf = new RandomizeVcf(sampleNames, random);

  @Test
  public void testRandomizeVariant() {
    VariantContext randomizedVariant = randomizeVcf.randomizeVariant(variantContext);
    // For each sample/GT in the variant...
    // For each allele in the genotype...
    // ...is that allele from the bank of possible alleles at that position?
    // ignoring ref/var
    assertThat(randomizedVariant.getGenotypes().size()).isEqualTo(2);
    assertThat(randomizedVariant.getGenotypes().getSampleNames())
        .isEqualTo(new HashSet<>(sampleNames));

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
  public void testRandomizeAlleles() {
    List<Allele> alleles = randomizeVcf.randomizeAlleles(variantContext);
    assertThat(alleles.size()).isEqualTo(2); // humans ought to be diploid.
    alleles.forEach(allele -> assertThat(variantContext.getAlleles()).contains(allele));
  }
}
