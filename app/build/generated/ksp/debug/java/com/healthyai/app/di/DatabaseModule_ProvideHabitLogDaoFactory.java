package com.healthyai.app.di;

import com.healthyai.app.data.local.AppDatabase;
import com.healthyai.app.data.local.dao.HabitLogDao;
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
public final class DatabaseModule_ProvideHabitLogDaoFactory implements Factory<HabitLogDao> {
  private final Provider<AppDatabase> dbProvider;

  public DatabaseModule_ProvideHabitLogDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public HabitLogDao get() {
    return provideHabitLogDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideHabitLogDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvideHabitLogDaoFactory(dbProvider);
  }

  public static HabitLogDao provideHabitLogDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideHabitLogDao(db));
  }
}
