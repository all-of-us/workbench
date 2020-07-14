package org.pmiops.workbench.genomics;

import static com.google.common.truth.Truth.assertThat;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import java.io.File;
import java.util.List;
import org.junit.Test;

public class RandomizeVcfTest {
  private static final VCFFileReader reader = new VCFFileReader(new File("resources/NA12878_204126160130_R01C01.toy.vcf.gz"));
  private static final VariantContext variantContext = reader.iterator().next();

  @Test
  public void TestRandomizeVariant() {
    VariantContext randomizedVariant = RandomizeVcf.randomizeVariant(variantContext);
    assertThat(randomizedVariant.getAlternateAlleles().get(0)).isEqualTo(Allele.ALT_A);
  }

  @Test
  public void TestRandomizeAlleles() {
    List<Allele> alleles = RandomizeVcf.randomizeAlleles(variantContext.getGenotypes().get(0).getAlleles(), variantContext.getReference());
    assertThat(alleles.size()).isEqualTo(2);
    alleles.forEach(allele -> assertThat(allele).isNotEqualTo(Allele.ALT_C));
  }
}
