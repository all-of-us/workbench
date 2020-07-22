package org.pmiops.workbench.genomics;

import com.google.common.annotations.VisibleForTesting;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.GenotypeBuilder;
import htsjdk.variant.variantcontext.GenotypesContext;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.VariantContextBuilder;
import htsjdk.variant.variantcontext.writer.VariantContextWriter;
import htsjdk.variant.vcf.VCFHeader;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.hellbender.cmdline.StandardArgumentDefinitions;
import org.broadinstitute.hellbender.engine.FeatureContext;
import org.broadinstitute.hellbender.engine.ReadsContext;
import org.broadinstitute.hellbender.engine.ReferenceContext;
import org.broadinstitute.hellbender.engine.VariantWalker;
import picard.cmdline.programgroups.VariantManipulationProgramGroup;

@CommandLineProgramProperties(
    summary = "Generates random variant alleles at the same contigs as an example VCF",
    oneLineSummary = "Randomizes a VCF",
    programGroup = VariantManipulationProgramGroup.class)
public class RandomizeVcf extends VariantWalker {
  @Argument(
      doc = "Output vcf name.",
      fullName = StandardArgumentDefinitions.OUTPUT_LONG_NAME,
      shortName = StandardArgumentDefinitions.OUTPUT_SHORT_NAME)
  protected File outputVcf;

  @Argument(doc = "Sample name suffix.", fullName = "SAMPLE_NAME_SUFFIX", shortName = "S")
  protected static String sampleNameSuffix;

  static Random random = new Random();

  private VariantContextWriter vcfWriter;

  @Override
  public void apply(
      VariantContext variant,
      ReadsContext readsContext,
      ReferenceContext referenceContext,
      FeatureContext featureContext) {
    vcfWriter.add(randomizeVariant(variant));
  }

  @Override
  public void onTraversalStart() {
    final VCFHeader inputHeader = getHeaderForVariants();
    final List<String> newSampleNames =
        inputHeader.getSampleNamesInOrder().stream()
            .map(RandomizeVcf::appendSuffixToSampleName)
            .collect(Collectors.toList());
    final VCFHeader outputHeader =
        new VCFHeader(inputHeader.getMetaDataInInputOrder(), newSampleNames);
    vcfWriter = this.createVCFWriter(outputVcf);
    vcfWriter.writeHeader(outputHeader);
  }

  @Override
  public void closeTool() {
    if (vcfWriter != null) {
      vcfWriter.close();
    }
  }

  @VisibleForTesting
  protected static VariantContext randomizeVariant(VariantContext variant) {
    // This initializes most of the VariantContextBuilder fields to what they were in the original
    // variant.
    // We just want to change the genotypes and quality score.
    VariantContextBuilder variantContextBuilder = new VariantContextBuilder(variant);
    variantContextBuilder.alleles(variant.getAlleles());

    List<Genotype> randomizedGenotypes =
        variant.getGenotypes().stream()
            .map(genotype -> randomizeGenotype(variant, genotype))
            .collect(Collectors.toList());
    GenotypesContext randomizedGenotypesContext =
        GenotypesContext.create(new ArrayList<>(randomizedGenotypes));

    variantContextBuilder.genotypes(randomizedGenotypesContext);

    // We want kind of random error. If there's no error, have no error for the new variant as well.
    // If there's error, fuzz the error.
    if (variant.hasLog10PError()) {
      variantContextBuilder.log10PError(random.nextDouble() * variant.getLog10PError());
    }

    return variantContextBuilder.make();
  }

  protected static Genotype randomizeGenotype(VariantContext variantContext, Genotype genotype) {
    GenotypeBuilder genotypeBuilder = new GenotypeBuilder();
    genotypeBuilder.copy(genotype);
    genotypeBuilder.name(appendSuffixToSampleName(genotype.getSampleName()));
    genotypeBuilder.alleles(randomizeAlleles(variantContext, genotype.getAlleles()));
    return genotypeBuilder.make();
  }

  @VisibleForTesting
  protected static List<Allele> randomizeAlleles(
      VariantContext variantContext, List<Allele> genotypeAlleles) {
    /*
     * NA12878 was run on the All of Us genotyping array in order to get an example of a VCF run
     * on this array. Here are the genotypes from that run and how often they appear:
     * 27629    ./.
     * 1518493  0/0
     * 106755   0/1
     * 117349   1/0
     * 138615   1/1
     * 1123     2/2
     * 1909964  total
     *
     * Percentage wise, this comes out to:
     * .0145 ./.
     * .7950 0/0
     * .0559 0/1
     * .0614 1/0
     * .0726 1/1
     * .0006 2/2
     *
     * We're going to pick from those GTs, weighted by frequency of appearance.
     */
    List<Allele> alleles = new ArrayList<>();
    double genotypeTypeIndex = random.nextDouble();
    if (genotypeTypeIndex < .0145) {
      // double no-call
      return alleles;
    } else if (genotypeTypeIndex < .8095) {
      // homref
      alleles.add(variantContext.getReference());
      alleles.add(variantContext.getReference());
    } else if (genotypeTypeIndex < .8654) {
      // 0/1 het
      alleles.add(variantContext.getReference());
      alleles.add(variantContext.getAlternateAllele(0));
    } else if (genotypeTypeIndex < .9268) {
      // 1/0 het
      alleles.add(variantContext.getAlternateAllele(0));
      alleles.add(variantContext.getReference());
    } else if (
        genotypeTypeIndex < .9994
        || (
            genotypeTypeIndex >= .9994
            && variantContext.getAlternateAlleles().size() == 1
        )
    ) {
      // homvar
      alleles.add(variantContext.getAlternateAllele(0));
      alleles.add(variantContext.getAlternateAllele(0));
    } else {
      // homvar, but the rarer alt
      alleles.add(variantContext.getAlternateAllele(1));
      alleles.add(variantContext.getAlternateAllele(1));
    }
    return alleles;
  }

  private static String appendSuffixToSampleName(String sampleName) {
    return sampleName + "." + sampleNameSuffix;
  }
}
