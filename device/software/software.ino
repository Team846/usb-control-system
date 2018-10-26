// PIN SETUP CODE
// pwm pins: 4, 14, 15
#define motorPin1 4
#define motorPin2 14
#define motorPin3 15
#define directionPin1 1
#define directionPin2 2
#define directionPin3 3

// encoder pins: 5, 6, 7, 8
#define encoderPin1A 5
#define encoderPin1B 6
#define encoderPin2A 7
#define encoderPin2B 8

// analog pins: 11 to 21
#define analogPin1 19
#define analogPin2 20
#define analogPin3 21

#define ledPin 11

//####################################################################################################################//
//###################################################### SERVOS ######################################################//
//####################################################################################################################//

#include <PWMServo.h> // docs: https://www.pjrc.com/teensy/td_libs_Servo.html
PWMServo servo1;
PWMServo servo2;
PWMServo servo3;

void servo_setup() {
  servo1.attach(motorPin1);
  servo2.attach(motorPin2);
  servo3.attach(motorPin3);

  digitalWrite(3, HIGH);
}

/**
   converts a duty cycle into an "angle" that servo library wants
   scales the value from [-100, 100] to [0, 180]
*/
uint16_t servo_convertForLibrary(const int16_t dc) {
  return (dc + 100) * 9 / 10;
}

void servo_write(const int8_t duty1, const int8_t duty2, const int8_t duty3) {
  servo1.write(servo_convertForLibrary(duty1));
  servo2.write(servo_convertForLibrary(duty2));
  servo3.write(servo_convertForLibrary(duty3));
}

//####################################################################################################################//
//############################################## DIRECTION + DUTY CYCLE ##############################################//
//####################################################################################################################//

#include <TimerOne.h>
void ddc_setup() {
  Timer1.initialize(20); // 50kHz
  Timer1.pwm(motorPin1, 0);
  Timer1.pwm(motorPin2, 0);
  Timer1.pwm(motorPin3, 0);

  pinMode(directionPin1, OUTPUT);
  pinMode(directionPin2, OUTPUT);
  pinMode(directionPin3, OUTPUT);
}

/**
   converts a duty cycle into an "angle" that servo library wants
   scales the value from [-100, 100] to [0, 1023]
*/
uint16_t ddc_convertForLibrary(const int16_t dc) {
  return abs(dc) * 93 / 100 * 11; // avoid overflow, 93 * 11 = 1023
}

void ddc_write(const int8_t duty1, const int8_t duty2, const int8_t duty3) {
  digitalWrite(directionPin1, duty1 < 0 ? LOW : HIGH);
  digitalWrite(directionPin2, duty2 < 0 ? LOW : HIGH);
  digitalWrite(directionPin3, duty3 < 0 ? LOW : HIGH);

  Timer1.setPwmDuty(motorPin1, ddc_convertForLibrary(duty1));
  Timer1.setPwmDuty(motorPin2, ddc_convertForLibrary(duty2));
  Timer1.setPwmDuty(motorPin3, ddc_convertForLibrary(duty3));
}

//####################################################################################################################//
//##################################################### ENCODERS #####################################################//
//####################################################################################################################//

//#define ENCODER_OPTIMIZE_INTERRUPTS // may conflict with other libraries using `attachInterrupt()`
//#include <Encoder.h> // docs: https://www.pjrc.com/teensy/td_libs_Encoder.html#optimize
//Encoder encoder1(encoderPin1A, encoderPin1B);
//Encoder encoder2(encoderPin2A, encoderPin2B);

//####################################################################################################################//
//######################################################## USB #######################################################//
//####################################################################################################################//

#define TX_SIZE 34
#define RX_SIZE 8
#define IO_TIMEOUT 100

uint8_t sendBuffer[TX_SIZE] = {0};
uint8_t recieveBuffer[RX_SIZE] = {0};

// every integer primitive in Java is signed
// so we must use signed numbers here
void usb_write(
  const int32_t encoder1_ticks = 0, const int32_t encoder1_period = 0, 
  const int32_t encoder2_ticks = 0, const int32_t encoder2_period = 0,
  const int16_t analog1 = 0, const int16_t analog2 = 0, const int16_t analog3 = 0,
  const int8_t servo1 = 0, const int8_t servo2 = 0, const int8_t servo3 = 0, const int8_t mode = 0,
  const int64_t timeStamp = 0
) {
  memcpy(sendBuffer + 0, &encoder1_ticks, 4);
  memcpy(sendBuffer + 4, &encoder1_period, 4);
  
  memcpy(sendBuffer + 8, &encoder2_ticks, 4);
  memcpy(sendBuffer + 12, &encoder2_period, 4);

  memcpy(sendBuffer + 16, &analog1, 2);
  memcpy(sendBuffer + 18, &analog2, 2);
  memcpy(sendBuffer + 20, &analog3, 2);

  memcpy(sendBuffer + 22, &servo1, 1);
  memcpy(sendBuffer + 23, &servo2, 1);
  memcpy(sendBuffer + 24, &servo3, 1);
  memcpy(sendBuffer + 25, &mode, 1);

  memcpy(sendBuffer + 26, &timeStamp, 8);

  Serial.write(sendBuffer, TX_SIZE);
  Serial.send_now();
}

void usb_read() {
  const uint64_t loopStart = millis();
  
  while (Serial.available() < RX_SIZE) {
    if (millis() - loopStart > IO_TIMEOUT) {
      digitalWrite(ledPin, HIGH);
      memset(recieveBuffer, 0, RX_SIZE);
      return;
    }
  }
  digitalWrite(ledPin, LOW);

  for (int i = 0; i < RX_SIZE; i++) {
    recieveBuffer[i] = Serial.read();
  }

  while (Serial.read() != -1);
}

int8_t usb_getOutput(const uint8_t motorNumber) {
  return recieveBuffer[motorNumber];
}

int8_t usb_getMode() {
  return recieveBuffer[0];
}

//####################################################################################################################//
//####################################################### MAIN #######################################################//
//####################################################################################################################//

#define SERVO_MODE 1
#define DDC_MODE 2
uint8_t mode = 0;

#define ledPin 11

void setup() { // called by arduino main
  
  pinMode(ledPin, OUTPUT);
  digitalWrite(ledPin, HIGH);
  
  Serial.begin(115200);
  
  while (!Serial.dtr());
  do {
    usb_read();
    mode = usb_getMode();
  }
  while(mode == 0);

  if (mode == DDC_MODE) ddc_setup();
  else servo_setup();
  
  encoder_setup();
  
  digitalWrite(ledPin, LOW);
}

void loop() { // called by arduino main
  const uint64_t loopStart = micros();
  
  usb_read();

  const int8_t m1 = usb_getOutput(1);
  const int8_t m2 = usb_getOutput(2);
  const int8_t m3 = usb_getOutput(3);

  if (mode == DDC_MODE) ddc_write(m1, m2, m3);
  else servo_write(m1, m2, m3);

  const uint64_t timeStamp = micros();

  usb_write(
    encoder1_getTicks(),//encoder1.read(),
    encoder1_getPeriod(),
    encoder2_getTicks(),//encoder2.read(),
    encoder2_getPeriod(),
    analogRead(analogPin1), // uint10_t casted to int16_t
    analogRead(analogPin2),
    analogRead(analogPin3),
    m1, m2, m3,
    mode,
    timeStamp               // uint64_t casted to int64_t
  );

  while(micros() - loopStart < 5000);
}
