package com.healthyai.app.domain.usecase;

import com.healthyai.app.domain.repository.HabitRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class LogHabitUseCase_Factory implements Factory<LogHabitUseCase> {
  private final Provider<HabitRepository> habitRepositoryProvider;

  public LogHabitUseCase_Factory(Provider<HabitRepository> habitRepositoryProvider) {
    this.habitRepositoryProvider = habitRepositoryProvider;
  }

  @Override
  public LogHabitUseCase get() {
    return newInstance(habitRepositoryProvider.get());
  }

  public static LogHabitUseCase_Factory create(Provider<HabitRepository> habitRepositoryProvider) {
    return new LogHabitUseCase_Factory(habitRepositoryProvider);
  }

  public static LogHabitUseCase newInstance(HabitRepository habitRepository) {
    return new LogHabitUseCase(habitRepository);
  }
}
