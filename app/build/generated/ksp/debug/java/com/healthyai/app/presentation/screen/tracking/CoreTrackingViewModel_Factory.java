package com.healthyai.app.presentation.screen.tracking;

import com.healthyai.app.domain.usecase.health.GetHealthDataUseCase;
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
public final class CoreTrackingViewModel_Factory implements Factory<CoreTrackingViewModel> {
  private final Provider<GetHealthDataUseCase> getHealthDataUseCaseProvider;

  public CoreTrackingViewModel_Factory(
      Provider<GetHealthDataUseCase> getHealthDataUseCaseProvider) {
    this.getHealthDataUseCaseProvider = getHealthDataUseCaseProvider;
  }

  @Override
  public CoreTrackingViewModel get() {
    return newInstance(getHealthDataUseCaseProvider.get());
  }

  public static CoreTrackingViewModel_Factory create(
      Provider<GetHealthDataUseCase> getHealthDataUseCaseProvider) {
    return new CoreTrackingViewModel_Factory(getHealthDataUseCaseProvider);
  }

  public static CoreTrackingViewModel newInstance(GetHealthDataUseCase getHealthDataUseCase) {
    return new CoreTrackingViewModel(getHealthDataUseCase);
  }
}
