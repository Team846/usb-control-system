volatile int32_t encoder1_ticks;
volatile uint32_t encoder1_period; // microseconds
volatile uint64_t encoder1_lastTick; // microseconds

volatile int32_t encoder2_ticks;
volatile uint32_t encoder2_period; // microseconds
volatile uint64_t encoder2_lastTick; // microseconds

void encoder_setup() {
  attachInterrupt(digitalPinToInterrupt(encoderPin1A), encoder1_interrupt, RISING);
  attachInterrupt(digitalPinToInterrupt(encoderPin2A), encoder2_interrupt, RISING);
}

int32_t encoder1_getTicks() { return encoder1_ticks; }
uint32_t encoder1_getPeriod() { return encoder1_period; }

int32_t encoder2_getTicks() { return encoder2_ticks; }
uint32_t encoder2_getPeriod() { return encoder2_period; }

void encoder1_interrupt() {
  const uint64_t t = micros();

  if (digitalRead(encoderPin1B)) encoder1_ticks--;
  else encoder1_ticks++;

  encoder1_period = t - encoder1_lastTick;
  encoder1_lastTick = t;
}

void encoder2_interrupt() {
  const uint64_t t = micros();

  if (digitalRead(encoderPin2B)) encoder2_ticks--;
  else encoder2_ticks++;

  encoder2_period = t - encoder2_lastTick;
  encoder2_lastTick = t;
}
