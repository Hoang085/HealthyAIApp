package com.healthyai.app.data.repository;

import com.healthyai.app.data.healthconnect.HealthConnectDataSource;
import com.healthyai.app.data.local.dao.health.HealthDataDao;
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
  private final Provider<HealthConnectDataSource> dataSourceProvider;

  private final Provider<HealthDataDao> healthDataDaoProvider;

  public HealthDataRepositoryImpl_Factory(Provider<HealthConnectDataSource> dataSourceProvider,
      Provider<HealthDataDao> healthDataDaoProvider) {
    this.dataSourceProvider = dataSourceProvider;
    this.healthDataDaoProvider = healthDataDaoProvider;
  }

  @Override
  public HealthDataRepositoryImpl get() {
    return newInstance(dataSourceProvider.get(), healthDataDaoProvider.get());
  }

  public static HealthDataRepositoryImpl_Factory create(
      Provider<HealthConnectDataSource> dataSourceProvider,
      Provider<HealthDataDao> healthDataDaoProvider) {
    return new HealthDataRepositoryImpl_Factory(dataSourceProvider, healthDataDaoProvider);
  }

  public static HealthDataRepositoryImpl newInstance(HealthConnectDataSource dataSource,
      HealthDataDao healthDataDao) {
    return new HealthDataRepositoryImpl(dataSource, healthDataDao);
  }
}
