package com.healthyai.app.domain.usecase.health;

import com.healthyai.app.domain.repository.HealthDataRepository;
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
public final class GetHealthDataUseCase_Factory implements Factory<GetHealthDataUseCase> {
  private final Provider<HealthDataRepository> repositoryProvider;

  public GetHealthDataUseCase_Factory(Provider<HealthDataRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public GetHealthDataUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static GetHealthDataUseCase_Factory create(
      Provider<HealthDataRepository> repositoryProvider) {
    return new GetHealthDataUseCase_Factory(repositoryProvider);
  }

  public static GetHealthDataUseCase newInstance(HealthDataRepository repository) {
    return new GetHealthDataUseCase(repository);
  }
}
