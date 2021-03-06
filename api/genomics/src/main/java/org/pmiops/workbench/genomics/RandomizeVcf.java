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
import java.io.IOException;
import java.nio.file.Files;
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

  @Argument(
      doc = "Newline separated file of sample names",
      fullName = "sample-names",
      shortName = "SN")
  protected File sampleNamesFile;

  private List<String> sampleNames;

  private Random random = new Random();

  private VariantContextWriter vcfWriter;

  public static void main(String[] argv) {
    new RandomizeVcf().instanceMain(argv);
  }

  public RandomizeVcf() {
    super();
  }

  @VisibleForTesting
  protected RandomizeVcf(List<String> sampleNames, Random random) {
    super();
    this.sampleNames = sampleNames;
    this.random = random;
  }

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
    try {
      this.sampleNames = Files.lines(sampleNamesFile.toPath()).collect(Collectors.toList());
    } catch (IOException e) {
      throw new RuntimeException("Could not open sample names file", e);
    }
    final VCFHeader outputHeader =
        new VCFHeader(inputHeader.getMetaDataInInputOrder(), this.sampleNames);
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
  protected VariantContext randomizeVariant(VariantContext variant) {
    // This initializes most of the VariantContextBuilder fields to what they were in the original
    // variant. We just want to change the genotypes and quality score.
    VariantContextBuilder variantContextBuilder = new VariantContextBuilder(variant);
    variantContextBuilder.alleles(variant.getAlleles());

    List<Genotype> randomizedGenotypes =
        this.sampleNames.stream()
            .map(name -> randomizeGenotype(variant, variant.getGenotype(0), name))
            .collect(Collectors.toList());

    GenotypesContext randomizedGenotypesContext =
        GenotypesContext.create(new ArrayList<>(randomizedGenotypes));

    variantContextBuilder.genotypes(randomizedGenotypesContext);

    return variantContextBuilder.make();
  }

  @VisibleForTesting
  protected Genotype randomizeGenotype(
      VariantContext variantContext, Genotype genotype, String sampleName) {
    GenotypeBuilder genotypeBuilder =
        new GenotypeBuilder()
            .copy(genotype)
            .name(sampleName)
            .alleles(randomizeAlleles(variantContext));
    return genotypeBuilder.make();
  }

  @VisibleForTesting
  protected List<Allele> randomizeAlleles(VariantContext variantContext) {
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
    if (variantContext.getAlternateAlleles().size() == 2) {
      // sum of probabilities of bi-allelic no-call / homref
      if (genotypeTypeIndex < .8240) {
        // double no-call (or ref, if we're at a tri-allelic site, but we don't differentiate)
        alleles.add(Allele.NO_CALL);
        alleles.add(Allele.NO_CALL);
        return alleles;
      } else if (genotypeTypeIndex < .8654) {
        // 1/2 het
        alleles.add(variantContext.getAlternateAllele(0));
        alleles.add(variantContext.getAlternateAllele(1));
      } else if (genotypeTypeIndex < .9268) {
        // 2/1 het
        alleles.add(variantContext.getAlternateAllele(1));
        alleles.add(variantContext.getAlternateAllele(0));
      } else {
        // homvar, but the rarer alt
        alleles.add(variantContext.getAlternateAllele(1));
        alleles.add(variantContext.getAlternateAllele(1));
      }
    } else {
      if (genotypeTypeIndex < .0145) {
        // double no-call
        alleles.add(Allele.NO_CALL);
        alleles.add(Allele.NO_CALL);
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
      } else {
        // homvar
        alleles.add(variantContext.getAlternateAllele(0));
        alleles.add(variantContext.getAlternateAllele(0));
      }
    }
    return alleles;
  }
}
