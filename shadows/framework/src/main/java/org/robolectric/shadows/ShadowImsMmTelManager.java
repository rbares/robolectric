package org.robolectric.shadows;

import android.Manifest;
import android.annotation.CallbackExecutor;
import android.annotation.RequiresPermission;
import android.annotation.SystemApi;
import android.os.Build.VERSION_CODES;
import android.support.annotation.NonNull;
import android.telephony.ims.ImsException;
import android.telephony.ims.ImsMmTelManager;
import android.telephony.ims.ImsMmTelManager.CapabilityCallback;
import android.telephony.ims.ImsMmTelManager.RegistrationCallback;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.feature.MmTelFeature.MmTelCapabilities;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import android.util.ArrayMap;
import java.util.Map;
import java.util.concurrent.Executor;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/**
 * Supports IMS by default. IMS unregistered by default.
 *
 * @see #setImsAvailableOnDevice(boolean)
 * @see #setImsRegistered(int)
 */
@Implements(
    value = ImsMmTelManager.class,
    minSdk = VERSION_CODES.Q,
    looseSignatures = true,
    isInAndroidSdk = false)
@SystemApi
public class ShadowImsMmTelManager {

  private final Map<RegistrationCallback, Executor> registrationCallbackExecutorMap =
      new ArrayMap<>();
  private final Map<CapabilityCallback, Executor> capabilityCallbackExecutorMap = new ArrayMap<>();
  private boolean imsAvailableOnDevice = true;
  private MmTelCapabilities mmTelCapabilitiesAvailable;
  private int imsRegistrationTech = ImsRegistrationImplBase.REGISTRATION_TECH_NONE;

  /**
   * Sets whether IMS is available on the device. Setting this to false will cause {@link
   * ImsException} to be thrown whenever methods requiring IMS support are invoked including {@link
   * #registerImsRegistrationCallback(Executor, RegistrationCallback)} and {@link
   * #registerMmTelCapabilityCallback(Executor, CapabilityCallback)}.
   */
  public void setImsAvailableOnDevice(boolean imsAvailableOnDevice) {
    this.imsAvailableOnDevice = imsAvailableOnDevice;
  }

  @RequiresPermission(Manifest.permission.READ_PRIVILEGED_PHONE_STATE)
  @Implementation
  protected void registerImsRegistrationCallback(
      @NonNull @CallbackExecutor Executor executor, @NonNull RegistrationCallback c)
      throws ImsException {
    if (!imsAvailableOnDevice) {
      throw new ImsException(
          "IMS not available on device.", ImsException.CODE_ERROR_UNSUPPORTED_OPERATION);
    }
    registrationCallbackExecutorMap.put(c, executor);
  }

  @RequiresPermission(Manifest.permission.READ_PRIVILEGED_PHONE_STATE)
  @Implementation
  protected void unregisterImsRegistrationCallback(@NonNull RegistrationCallback c) {
    registrationCallbackExecutorMap.remove(c);
  }

  /**
   * Triggers {@link RegistrationCallback#onRegistering(int)} for all registered {@link
   * RegistrationCallback} callbacks.
   *
   * @see #registerImsRegistrationCallback(Executor, RegistrationCallback)
   */
  public void setImsRegistering(int imsRegistrationTech) {
    for (Map.Entry<RegistrationCallback, Executor> entry :
        registrationCallbackExecutorMap.entrySet()) {
      entry.getValue().execute(() -> entry.getKey().onRegistering(imsRegistrationTech));
    }
  }

  /**
   * Triggers {@link RegistrationCallback#onRegistered(int)} for all registered {@link
   * RegistrationCallback} callbacks.
   *
   * @see #registerImsRegistrationCallback(Executor, RegistrationCallback)
   */
  public void setImsRegistered(int imsRegistrationTech) {
    this.imsRegistrationTech = imsRegistrationTech;
    for (Map.Entry<RegistrationCallback, Executor> entry :
        registrationCallbackExecutorMap.entrySet()) {
      entry.getValue().execute(() -> entry.getKey().onRegistered(imsRegistrationTech));
    }
  }

  /**
   * Triggers {@link RegistrationCallback#onUnregistered(ImsReasonInfo)} for all registered {@link
   * RegistrationCallback} callbacks.
   *
   * @see #registerImsRegistrationCallback(Executor, RegistrationCallback)
   */
  public void setImsUnregistered(@NonNull ImsReasonInfo imsReasonInfo) {
    this.imsRegistrationTech = ImsRegistrationImplBase.REGISTRATION_TECH_NONE;
    for (Map.Entry<RegistrationCallback, Executor> entry :
        registrationCallbackExecutorMap.entrySet()) {
      entry.getValue().execute(() -> entry.getKey().onUnregistered(imsReasonInfo));
    }
  }

  @RequiresPermission(Manifest.permission.READ_PRIVILEGED_PHONE_STATE)
  @Implementation
  protected void registerMmTelCapabilityCallback(
      @NonNull @CallbackExecutor Executor executor, @NonNull CapabilityCallback c)
      throws ImsException {
    if (!imsAvailableOnDevice) {
      throw new ImsException(
          "IMS not available on device.", ImsException.CODE_ERROR_UNSUPPORTED_OPERATION);
    }
    capabilityCallbackExecutorMap.put(c, executor);
  }

  @RequiresPermission(Manifest.permission.READ_PRIVILEGED_PHONE_STATE)
  @Implementation
  protected void unregisterMmTelCapabilityCallback(@NonNull CapabilityCallback c) {
    capabilityCallbackExecutorMap.remove(c);
  }

  @RequiresPermission(Manifest.permission.READ_PRIVILEGED_PHONE_STATE)
  @Implementation
  protected boolean isAvailable(
      @MmTelCapabilities.MmTelCapability int capability,
      @ImsRegistrationImplBase.ImsRegistrationTech int imsRegTech) {
    // Available if MmTelCapability enabled and IMS registered under same tech
    return mmTelCapabilitiesAvailable.isCapable(capability) && imsRegTech == imsRegistrationTech;
  }

  /**
   * Sets the available {@link MmTelCapabilities}. Only invokes {@link
   * CapabilityCallback#onCapabilitiesStatusChanged(MmTelCapabilities)} if IMS has been registered
   * using {@link #setImsUnregistered(ImsReasonInfo)}.
   */
  public void setMmTelCapabilitiesAvailable(@NonNull MmTelCapabilities capabilities) {
    this.mmTelCapabilitiesAvailable = capabilities;
    if (imsRegistrationTech != ImsRegistrationImplBase.REGISTRATION_TECH_NONE) {
      for (Map.Entry<CapabilityCallback, Executor> entry :
          capabilityCallbackExecutorMap.entrySet()) {
        entry.getValue().execute(() -> entry.getKey().onCapabilitiesStatusChanged(capabilities));
      }
    }
  }
}
