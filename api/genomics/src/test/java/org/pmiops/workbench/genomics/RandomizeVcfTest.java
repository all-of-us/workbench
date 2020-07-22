package org.pmiops.workbench.genomics;

import static com.google.common.truth.Truth.assertThat;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import java.io.File;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;

public class RandomizeVcfTest {
  private static final VCFFileReader reader =
      new VCFFileReader(new File("src/test/resources/NA12878_204126160130_R01C01.toy.vcf.gz"));
  private static final VariantContext variantContext = reader.iterator().next();
  private static final RandomizeVcf randomizeVcf = new RandomizeVcf();

  @BeforeClass
  public static void setUp() {
    randomizeVcf.sampleNameSuffix = "0";
  }

  @Test
  public void TestRandomizeVariant() {
    VariantContext randomizedVariant = randomizeVcf.randomizeVariant(variantContext);
    // For each sample/GT in the variant...
    randomizedVariant.getGenotypes().forEach(
        genotype -> {
          // For each allele in the genotype...
          genotype.getAlleles().forEach(
              allele -> {
                // ...is that allele from the bank of possible alleles at that position? ignoring
                // ref/varus

                assertThat(
                    variantContext.getAlleles().stream()
                        .anyMatch(vcAllele -> vcAllele.basesMatch(allele))
                ).isTrue();
              }
          );
        }
    );
  }

  @Test
  public void TestRandomizeGenotypes() {
    Genotype genotype = variantContext.getGenotype(0);
    Genotype randomizedGenotype = randomizeVcf.randomizeGenotype(variantContext, genotype);
    assertThat(randomizedGenotype.getSampleName()).isNotEqualTo(genotype.getSampleName());
  }

  @Test
  public void TestRandomizeAlleles() {
    List<Allele> alleles =
        randomizeVcf.randomizeAlleles(variantContext, variantContext.getGenotype(0).getAlleles());
    assertThat(alleles.size()).isEqualTo(2); // humans ought to be diploid.
    alleles.forEach(allele -> assertThat(variantContext.getAlleles()).contains(allele));
  }
}
