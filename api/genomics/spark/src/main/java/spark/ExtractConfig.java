package spark;

public class ExtractConfig {
  public final Long minLocation;
  public final Long maxLocation;
  public final String outputFilename;

  public ExtractConfig(Long minLocation, Long maxLocation, String outputFilename) {
    this.minLocation = minLocation;
    this.maxLocation = maxLocation;
    this.outputFilename = outputFilename;
  }
}
