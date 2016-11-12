package clearcl.ops.test;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import clearcl.ClearCL;
import clearcl.ClearCLBuffer;
import clearcl.ClearCLContext;
import clearcl.ClearCLDevice;
import clearcl.ClearCLImage;
import clearcl.backend.ClearCLBackendInterface;
import clearcl.backend.javacl.ClearCLBackendJavaCL;
import clearcl.enums.HostAccessType;
import clearcl.enums.ImageChannelDataType;
import clearcl.enums.ImageChannelOrder;
import clearcl.enums.KernelAccessType;
import clearcl.ops.Reductions;
import coremem.offheap.OffHeapMemory;
import coremem.types.NativeTypeEnum;

public class RecustionsTests
{

  @Test
  public void testMinMaxBufferF() throws IOException
  {
    ClearCLBackendInterface lClearCLBackend = new ClearCLBackendJavaCL();

    try (ClearCL lClearCL = new ClearCL(lClearCLBackend))
    {
      ClearCLDevice lBestGPUDevice = lClearCL.getBestGPUDevice();

      ClearCLContext lCreateContext = lBestGPUDevice.createContext();

      ClearCLBuffer lCLBuffer = lCreateContext.createBuffer(HostAccessType.ReadWrite,
                                                            KernelAccessType.ReadWrite,
                                                            NativeTypeEnum.Float,
                                                            2048 * 2048 + 1);

      OffHeapMemory lBuffer = OffHeapMemory.allocateFloats(lCLBuffer.getLength());

      float lJavaMin = Float.POSITIVE_INFINITY;
      float lJavaMax = Float.NEGATIVE_INFINITY;
      for (int i = 0; i < lCLBuffer.getLength(); i++)
      {
        float lValue = 1f / (1f + i);
        lJavaMin = Math.min(lJavaMin, lValue);
        lJavaMax = Math.max(lJavaMax, lValue);
        lBuffer.setFloatAligned(i, lValue);
      }

      //System.out.println("lJavaMin=" + lJavaMin);
      //System.out.println("lJavaMax=" + lJavaMax);

      lCLBuffer.readFrom(lBuffer, true);

      Reductions lReductions = new Reductions(lCreateContext.getDefaultQueue());

      float[] lOpenCLMinMax = lReductions.minmax(lCLBuffer, 3712);

      //System.out.println("lOpenCL Min=" + lOpenCLMinMax[0]);
      //System.out.println("lOpenCL Max=" + lOpenCLMinMax[1]);

      assertEquals(lJavaMin, lOpenCLMinMax[0], 0.0001);
      assertEquals(lJavaMax, lOpenCLMinMax[1], 0.0001);

      for (int r = 512; r < 1024 * 4; r += 256)
        benchmark(lCLBuffer, lReductions, r);

      lCLBuffer.close();
    }
  }

  private void benchmark(ClearCLBuffer lCLBuffer,
                         Reductions lReductions,
                         int lRed)
  {
    lReductions.minmax(lCLBuffer, lRed);

    long lNanoStart = System.nanoTime();
    int lRepeats = 32;
    for (int r = 0; r < lRepeats; r++)
      lReductions.minmax(lCLBuffer, lRed);
    long lNanoStop = System.nanoTime();

    double lElapsedTime = 10e-9 * (lNanoStop - lNanoStart) / lRepeats;

    /*System.out.format("elpased time: %g seconds for %d reductions\n",
                      lElapsedTime,
                      lRed);/**/
  }

  @Test
  public void testMinMaxImage1F() throws IOException
  {
    ClearCLBackendInterface lClearCLBackend = new ClearCLBackendJavaCL();

    try (ClearCL lClearCL = new ClearCL(lClearCLBackend))
    {
      ClearCLDevice lBestGPUDevice = lClearCL.getBestGPUDevice();

      ClearCLContext lCreateContext = lBestGPUDevice.createContext();

      ClearCLImage lClearCLImage = lCreateContext.createImage(HostAccessType.ReadWrite,
                                                              KernelAccessType.ReadWrite,
                                                              ImageChannelOrder.Intensity,
                                                              ImageChannelDataType.Float,
                                                              2048 + 1);

      testMinMaxWith(lCreateContext, lClearCLImage);

      lClearCLImage.close();
    }
  }

  @Test
  public void testMinMaxImage2F() throws IOException
  {
    ClearCLBackendInterface lClearCLBackend = new ClearCLBackendJavaCL();

    try (ClearCL lClearCL = new ClearCL(lClearCLBackend))
    {
      ClearCLDevice lBestGPUDevice = lClearCL.getBestGPUDevice();

      ClearCLContext lCreateContext = lBestGPUDevice.createContext();

      ClearCLImage lClearCLImage = lCreateContext.createImage(HostAccessType.ReadWrite,
                                                              KernelAccessType.ReadWrite,
                                                              ImageChannelOrder.Intensity,
                                                              ImageChannelDataType.Float,
                                                              2048 + 1,
                                                              2048 - 1);

      testMinMaxWith(lCreateContext, lClearCLImage);

      lClearCLImage.close();
    }
  }

  @Test
  public void testMinMaxImage3F() throws IOException
  {
    ClearCLBackendInterface lClearCLBackend = new ClearCLBackendJavaCL();

    try (ClearCL lClearCL = new ClearCL(lClearCLBackend))
    {
      ClearCLDevice lBestGPUDevice = lClearCL.getBestGPUDevice();

      ClearCLContext lCreateContext = lBestGPUDevice.createContext();

      ClearCLImage lClearCLImage = lCreateContext.createImage(HostAccessType.ReadWrite,
                                                              KernelAccessType.ReadWrite,
                                                              ImageChannelOrder.Intensity,
                                                              ImageChannelDataType.Float,
                                                              128 + 1,
                                                              128 - 1,
                                                              128 - 3);

      testMinMaxWith(lCreateContext, lClearCLImage);

      lClearCLImage.close();
    }
  }

  private void testMinMaxWith(ClearCLContext lCreateContext,
                              ClearCLImage lClearCLImage) throws IOException
  {
    OffHeapMemory lBuffer = OffHeapMemory.allocateFloats(lClearCLImage.getVolume());

    float lJavaMin = Float.POSITIVE_INFINITY;
    float lJavaMax = Float.NEGATIVE_INFINITY;
    for (int i = 0; i < lClearCLImage.getVolume(); i++)
    {
      float lValue = (1f+(i%127)) / 128;
      lJavaMin = Math.min(lJavaMin, lValue);
      lJavaMax = Math.max(lJavaMax, lValue);
      lBuffer.setFloatAligned(i, lValue);
    }

    //System.out.println("lJavaMin=" + lJavaMin);
    //System.out.println("lJavaMax=" + lJavaMax);

    lClearCLImage.readFrom(lBuffer, true);

    Reductions lReductions = new Reductions(lCreateContext.getDefaultQueue());

    //System.out.println("before minmax");
    float[] lOpenCLMinMax = lReductions.minmax(lClearCLImage, 32);
    //System.out.println("after minmax");

    //System.out.println("lOpenCL Min=" + lOpenCLMinMax[0]);
    //System.out.println("lOpenCL Max=" + lOpenCLMinMax[1]);

    assertEquals(lJavaMin, lOpenCLMinMax[0], 0.0000001);
    assertEquals(lJavaMax, lOpenCLMinMax[1], 0.0000001);
  }

}
