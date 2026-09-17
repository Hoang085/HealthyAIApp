package com.healthyai.app.worker;

import android.content.Context;
import androidx.work.WorkerParameters;
import dagger.internal.DaggerGenerated;
import dagger.internal.InstanceFactory;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class HealthSyncWorker_AssistedFactory_Impl implements HealthSyncWorker_AssistedFactory {
  private final HealthSyncWorker_Factory delegateFactory;

  HealthSyncWorker_AssistedFactory_Impl(HealthSyncWorker_Factory delegateFactory) {
    this.delegateFactory = delegateFactory;
  }

  @Override
  public HealthSyncWorker create(Context p0, WorkerParameters p1) {
    return delegateFactory.get(p0, p1);
  }

  public static Provider<HealthSyncWorker_AssistedFactory> create(
      HealthSyncWorker_Factory delegateFactory) {
    return InstanceFactory.create(new HealthSyncWorker_AssistedFactory_Impl(delegateFactory));
  }

  public static dagger.internal.Provider<HealthSyncWorker_AssistedFactory> createFactoryProvider(
      HealthSyncWorker_Factory delegateFactory) {
    return InstanceFactory.create(new HealthSyncWorker_AssistedFactory_Impl(delegateFactory));
  }
}
