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
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
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
    programGroup = VariantManipulationProgramGroup.class
)
public class RandomizeVcf extends VariantWalker {
  @Argument(doc = "Output vcf name.",
      fullName = StandardArgumentDefinitions.OUTPUT_LONG_NAME,
      shortName = StandardArgumentDefinitions.OUTPUT_SHORT_NAME)
  protected File outputVcf;

  @VisibleForTesting
  protected static Allele[] alleleBank = {
      Allele.ALT_A,
      Allele.ALT_C,
      Allele.ALT_G,
      Allele.ALT_T,
      Allele.NO_CALL
  };

  static Random random = new Random();

  private VariantContextWriter vcfWriter;

  @Override
  public void apply(VariantContext variant, ReadsContext readsContext,
      ReferenceContext referenceContext, FeatureContext featureContext) {
    vcfWriter.add(randomizeVariant(variant));
  }

  @Override
  public void onTraversalStart() {
    final VCFHeader inputHeader = getHeaderForVariants();
    vcfWriter = this.createVCFWriter(outputVcf);
    vcfWriter.writeHeader(inputHeader);
  }

  @Override
  public void closeTool() {
    if ( vcfWriter != null ) {
      vcfWriter.close();
    }
  }

  @VisibleForTesting
  protected static VariantContext randomizeVariant(VariantContext variant) {
    // This initializes most of the VariantContextBuilder fields to what they were in the original variant.
    // We just want to change the alleles, genotypes, and quality score.
    VariantContextBuilder variantContextBuilder = new VariantContextBuilder(variant);

    List<Genotype> randomizedGenotypes = variant.getGenotypes()
        .stream()
        .map(genotype -> randomizeGenotype(genotype, variant.getReference()))
        .collect(Collectors.toList());
    GenotypesContext randomizedGenotypesContext = GenotypesContext.create(new ArrayList<>(randomizedGenotypes));

    Set<Allele> randomizedAllelesInGenotype = randomizedGenotypesContext
        .stream()
        .flatMap(genotype -> genotype.getAlleles().stream())
        .collect(Collectors.toSet());
    // Can't have duplicate alleles in a VariantContext, so we have to remove the reference from this set
    randomizedAllelesInGenotype.remove(variant.getReference());

    List<Allele> randomizedAlleles = new ArrayList<>();
    randomizedAlleles.add(variant.getReference());
    randomizedAlleles.addAll(randomizedAllelesInGenotype);

    variantContextBuilder.genotypes(randomizedGenotypesContext);
    variantContextBuilder.alleles(randomizedAlleles);

    // We want kind of random error. If there's no error, have no error for the new variant as well.
    // If there's error, fuzz the error.
    if (variant.hasLog10PError()) {
      variantContextBuilder.log10PError(random.nextDouble() * variant.getLog10PError());
    }

    return variantContextBuilder.make();
  }

  protected static Genotype randomizeGenotype(Genotype genotype, Allele reference) {
    GenotypeBuilder genotypeBuilder = new GenotypeBuilder();
    genotypeBuilder.copy(genotype);
    genotypeBuilder.alleles(randomizeAlleles(genotype.getAlleles(), reference));
    return genotypeBuilder.make();
  }

  @VisibleForTesting
  protected static List<Allele> randomizeAlleles(List<Allele> alleles, Allele reference) {
    // Don't want to accidentally use the reference or else it wouldn't be a snp
    List<Allele> alleleBankMinusReference = Arrays
        .stream(alleleBank)
        .filter(allele -> !allele.basesMatch(reference))
        .collect(Collectors.toList());
    // We don't want to just stick no-calls everywhere, which we would if no-call was given equal chance
    // of occurring to all the other alleles, so we only use a no-call if the original had a no-call.
    final int max = alleles.contains(Allele.NO_CALL)
        ? alleleBankMinusReference.size() - 1
        : alleleBankMinusReference.size() - 2;
    // for each allele, pick a random allele from the allele bank
    return alleles.stream()
        .map(allele -> alleleBankMinusReference.get(random.nextInt(max)))
        .collect(Collectors.toList());
  }
}
