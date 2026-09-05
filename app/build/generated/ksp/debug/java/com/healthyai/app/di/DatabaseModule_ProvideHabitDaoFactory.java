package com.healthyai.app.di;

import com.healthyai.app.data.local.AppDatabase;
import com.healthyai.app.data.local.dao.HabitDao;
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
public final class DatabaseModule_ProvideHabitDaoFactory implements Factory<HabitDao> {
  private final Provider<AppDatabase> dbProvider;

  public DatabaseModule_ProvideHabitDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public HabitDao get() {
    return provideHabitDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideHabitDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvideHabitDaoFactory(dbProvider);
  }

  public static HabitDao provideHabitDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideHabitDao(db));
  }
}
