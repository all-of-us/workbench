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
    // The alleles list on the VariantContext has first the reference and then all possible
    // alternates.
    // For each genotype, we pick from among those possible alternates (or we put a no-call.)
    // We don't want to just stick no-calls everywhere, which we would if no-call was given equal
    // chance of occurring to all the other alleles, so we only use a no-call if the original had a
    // no-call.
    List<Allele> possibleAlleles = new ArrayList<>(variantContext.getAlleles());
    if (genotypeAlleles.contains(Allele.NO_CALL)) {
      possibleAlleles.add(Allele.NO_CALL);
    }
    return genotypeAlleles.stream()
        .map(allele -> possibleAlleles.get(random.nextInt(possibleAlleles.size())))
        .collect(Collectors.toList());
  }

  private static String appendSuffixToSampleName(String sampleName) {
    return sampleName + "." + sampleNameSuffix;
  }
}
