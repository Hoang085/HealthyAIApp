package com.healthyai.app.data.repository;

import com.healthyai.app.data.local.dao.HabitDao;
import com.healthyai.app.data.local.dao.HabitLogDao;
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
public final class HabitRepositoryImpl_Factory implements Factory<HabitRepositoryImpl> {
  private final Provider<HabitDao> habitDaoProvider;

  private final Provider<HabitLogDao> habitLogDaoProvider;

  public HabitRepositoryImpl_Factory(Provider<HabitDao> habitDaoProvider,
      Provider<HabitLogDao> habitLogDaoProvider) {
    this.habitDaoProvider = habitDaoProvider;
    this.habitLogDaoProvider = habitLogDaoProvider;
  }

  @Override
  public HabitRepositoryImpl get() {
    return newInstance(habitDaoProvider.get(), habitLogDaoProvider.get());
  }

  public static HabitRepositoryImpl_Factory create(Provider<HabitDao> habitDaoProvider,
      Provider<HabitLogDao> habitLogDaoProvider) {
    return new HabitRepositoryImpl_Factory(habitDaoProvider, habitLogDaoProvider);
  }

  public static HabitRepositoryImpl newInstance(HabitDao habitDao, HabitLogDao habitLogDao) {
    return new HabitRepositoryImpl(habitDao, habitLogDao);
  }
}
