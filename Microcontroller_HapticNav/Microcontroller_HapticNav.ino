char incomingByte;  // incoming data
int LEDleft = 13;      // LED pin
int LEDright = 9;
int LEDfront = 11;
char change = '0'; 

// Ultrasonic

const int trigPin = 4;
const int echoPin = 5;
long duration;
long distance;
 
void setup() {
  Serial.begin(9600); // initialization
  pinMode(LEDleft, OUTPUT);
  pinMode(LEDright, OUTPUT);
  pinMode(LEDfront, OUTPUT);

  // Ultrasonic
  pinMode(trigPin, OUTPUT); // Sets the trigPin as an Output
  pinMode(echoPin, INPUT); // Sets the echoPin as an Input

  //Serial.println("Press 1 to LED ON or 0 to LED OFF...");

}
 
void loop() {
  
   digitalWrite(trigPin, LOW); 
  delayMicroseconds(2);
  digitalWrite(trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin, LOW);
  duration = pulseIn(echoPin, HIGH);
  distance = (duration/2) / 29.1;

  if (distance < 30){
  digitalWrite(LEDfront, HIGH);
    delay(100);                  // waits for a second
    digitalWrite(LEDfront, LOW);    // sets the LED off
    delay(100);                  // waits for a second   
  }

  if(distance < 100 && distance > 30){
  digitalWrite(LEDfront, HIGH);
    delay(400);                  // waits for a second
    digitalWrite(LEDfront, LOW);    // sets the LED off
    delay(400);               
  }
  
  if (Serial.available() > 0) {  // if the data came
     // while (Serial.read() >= 0);
    incomingByte = Serial.read(); // read byte

    if(incomingByte == '1') { //Origin
       digitalWrite(LEDleft, HIGH); // if 0, switch LED on
       digitalWrite(LEDright, HIGH);
       delay (1000);
       digitalWrite(LEDleft, LOW); // if 0, switch LED on
       digitalWrite(LEDright, LOW);
       //Serial.println("LED ON. Press 0 to LED OFF!");
    }
    else if(incomingByte == '2') {   //Destination on the right
       digitalWrite(LEDfront, HIGH); // if 0, switch LED on
       digitalWrite(LEDright, HIGH); // if 0, switch LED on
       delay (1000);
       digitalWrite(LEDfront, LOW); // if 0, switch LED on
       digitalWrite(LEDright, LOW); 
      //Serial.println("LED ON. Press 0 to LED OFF!");
    }
     else if(incomingByte == '3') {  //Destination on the left
       digitalWrite(LEDfront, HIGH); // if 0, switch LED on
       digitalWrite(LEDleft, HIGH); // if 0, switch LED on
       delay (1000);
       digitalWrite(LEDfront, LOW); // if 0, switch LED on
       digitalWrite(LEDleft, LOW);
       //Serial.println("LED ON. Press 0 to LED OFF!");
    }
    else if(incomingByte == '4' && (distance < 100 && distance > 30)) {
    digitalWrite(LEDfront, HIGH);
    delay(400);                  // waits for a second
    digitalWrite(LEDfront, LOW);    // sets the LED off
    delay(400);         
  }
  else if(incomingByte == '4' && distance < 30) {
    digitalWrite(LEDfront, HIGH);
    delay(100);                  // waits for a second
    digitalWrite(LEDfront, LOW);    // sets the LED off
    delay(100);                  // waits for a second
  }
    
    else if(incomingByte == '4') { //Destination
       digitalWrite(LEDleft, HIGH); // if 0, switch LED on
       digitalWrite(LEDright, HIGH);
       digitalWrite(LEDfront, HIGH); // if 0, switch LED on
       delay (1000);
       digitalWrite(LEDleft, LOW); // if 0, switch LED on
       digitalWrite(LEDright, LOW);
       digitalWrite(LEDfront, LOW); // if 0, switch LED on
       //Serial.println("LED ON. Press 0 to LED OFF!");
    }
    else if(incomingByte == '5') {   //Turn right
       digitalWrite(LEDright, HIGH); // if 0, switch LED on
       delay (1000);
       digitalWrite(LEDright, LOW);
       Serial.println("LED ON. Press 0 to LED OFF!");
    }
    else if(incomingByte == '6') {   //Turn left
       digitalWrite(LEDleft, HIGH); // if 0, switch LED on
       delay (1000);
       digitalWrite(LEDleft, LOW);
       //Serial.println("LED ON. Press 0 to LED OFF!");
    } 
    else if(incomingByte == '7' && (distance < 100 && distance > 30)) {
    digitalWrite(LEDfront, HIGH);
    delay(400);                  // waits for a second
    digitalWrite(LEDfront, LOW);    // sets the LED off
    delay(400);         
  }
  else if(incomingByte == '7' && distance < 30) {
    digitalWrite(LEDfront, HIGH);
    delay(100);                  // waits for a second
    digitalWrite(LEDfront, LOW);    // sets the LED off
    delay(100);                  // waits for a second
  }
    else if(incomingByte == '7') {     //Continue going forward
       digitalWrite(LEDfront, HIGH); // if 0, switch LED on
       delay (1000);
       digitalWrite(LEDfront, LOW);
       //Serial.println("LED ON. Press 0 to LED OFF!");
    }          
  }

 
//  
//  if (distance < 30) {
//    digitalWrite(LEDfront, HIGH);
//    delay(100);                  // waits for a second
//    digitalWrite(LEDfront, LOW);    // sets the LED off
//    delay(100);                  // waits for a second
//  }
//
//  if (distance < 100 && distance > 30) {
//    digitalWrite(LEDfront, HIGH);
//    delay(400);                  // waits for a second
//    digitalWrite(LEDfront, LOW);    // sets the LED off
//    delay(400);                  // waits for a second
//  }
// //  delay (500);
}
