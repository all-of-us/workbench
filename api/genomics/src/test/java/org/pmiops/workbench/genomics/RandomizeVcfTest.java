package org.pmiops.workbench.genomics;

import static com.google.common.truth.Truth.assertThat;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import java.io.File;
import java.util.List;
import org.junit.Test;

public class RandomizeVcfTest {
  private static final VCFFileReader reader =
      new VCFFileReader(new File("src/test/resources/NA12878_204126160130_R01C01.toy.vcf.gz"));
  private static final VariantContext variantContext = reader.iterator().next();

  @Test
  public void TestRandomizeVariant() {
    VariantContext randomizedVariant = RandomizeVcf.randomizeVariant(variantContext);
    assertThat(randomizedVariant.getAlleles())
        .containsAllIn(randomizedVariant.getGenotype(0).getAlleles());
  }

  @Test
  public void TestRandomizeGenotypes() {
    Genotype genotype = variantContext.getGenotype(0);
    Genotype randomizedGenotype = RandomizeVcf.randomizeGenotype(variantContext, genotype);
    assertThat(randomizedGenotype.getSampleName()).isNotEqualTo(genotype.getSampleName());
  }

  @Test
  public void TestRandomizeAlleles() {
    List<Allele> alleles =
        RandomizeVcf.randomizeAlleles(variantContext, variantContext.getGenotype(0).getAlleles());
    assertThat(alleles.size()).isEqualTo(2);
    alleles.forEach(allele -> assertThat(variantContext.getAlleles()).contains(allele));
  }
}
