package org.pmiops.workbench.genomics;

import picard.arrays.CombineGenotypingArrayVcfs;

public class CombineGenotypingArrayVcfsEntryPoint {
  public static void main(String[] argv) {
    new CombineGenotypingArrayVcfs().instanceMain(argv);
  }
}
