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
public final class GetTodayHabitsUseCase_Factory implements Factory<GetTodayHabitsUseCase> {
  private final Provider<HabitRepository> habitRepositoryProvider;

  public GetTodayHabitsUseCase_Factory(Provider<HabitRepository> habitRepositoryProvider) {
    this.habitRepositoryProvider = habitRepositoryProvider;
  }

  @Override
  public GetTodayHabitsUseCase get() {
    return newInstance(habitRepositoryProvider.get());
  }

  public static GetTodayHabitsUseCase_Factory create(
      Provider<HabitRepository> habitRepositoryProvider) {
    return new GetTodayHabitsUseCase_Factory(habitRepositoryProvider);
  }

  public static GetTodayHabitsUseCase newInstance(HabitRepository habitRepository) {
    return new GetTodayHabitsUseCase(habitRepository);
  }
}
