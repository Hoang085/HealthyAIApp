package com.healthyai.app.di;

import com.healthyai.app.data.local.AppDatabase;
import com.healthyai.app.data.local.dao.health.HealthDataDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class DatabaseModule_ProvideHealthDataDaoFactory implements Factory<HealthDataDao> {
  private final Provider<AppDatabase> dbProvider;

  public DatabaseModule_ProvideHealthDataDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public HealthDataDao get() {
    return provideHealthDataDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideHealthDataDaoFactory create(
      Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvideHealthDataDaoFactory(dbProvider);
  }

  public static HealthDataDao provideHealthDataDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideHealthDataDao(db));
  }
}
