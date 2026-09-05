package com.healthyai.app.data.repository;

import com.healthyai.app.data.healthconnect.HealthConnectManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
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
public final class HealthDataRepositoryImpl_Factory implements Factory<HealthDataRepositoryImpl> {
  private final Provider<HealthConnectManager> managerProvider;

  public HealthDataRepositoryImpl_Factory(Provider<HealthConnectManager> managerProvider) {
    this.managerProvider = managerProvider;
  }

  @Override
  public HealthDataRepositoryImpl get() {
    return newInstance(managerProvider.get());
  }

  public static HealthDataRepositoryImpl_Factory create(
      Provider<HealthConnectManager> managerProvider) {
    return new HealthDataRepositoryImpl_Factory(managerProvider);
  }

  public static HealthDataRepositoryImpl newInstance(HealthConnectManager manager) {
    return new HealthDataRepositoryImpl(manager);
  }
}
