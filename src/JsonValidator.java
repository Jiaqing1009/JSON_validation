import java.io.*;

public class JsonValidator {

    private static int array_pointer;
    private static String string;
    private static char current_char;

    //Read a file and transfer it to a String by giving its file path
    private static String readFileToString(String filepath) throws IOException {
        StringBuilder sb = new StringBuilder();
        String s;
        BufferedReader br = new BufferedReader(new FileReader(filepath));
        while( (s = br.readLine()) != null) {
            sb.append(s + "\n");
        }
        br.close();
        return sb.toString();
    }

    //Custom some exceptions which is used to accept errors found inside the iteration
    public static class CustomException extends Exception {
        public CustomException(String message){
            super(message);
        }
    }

    //Return next char
    public static char nextChar() {
        if (array_pointer < 0 || array_pointer >= string.length()) {
            return 0;
        }
        current_char = string.charAt(array_pointer);
        array_pointer++;
        return current_char;
    }

    //Skip the space in the JSON file
    public static char nextRealChar() throws CustomException{
        do {
            nextChar();
        }while (current_char == '\n' || current_char == '\r' || current_char == ' ' || current_char == 0);
        if (current_char != 0 && (current_char < 32 || current_char == 127) ) {
            throw new CustomException("Invalid char found");
        }
        return current_char;
    }

    //validate the array structure
    public static boolean validateArray() throws CustomException {
        nextRealChar();
        if (current_char == ']') {
            return true; //empty array, return true
        } else if(current_char == ',') {
            throw new CustomException("Extra comma found"); //extra comma after '['
        }
        while(true){
            if (current_char == ']') {
                throw new CustomException("Extra comma found"); //extra comma, this is testing while it has iterations
            }
            else if (current_char == '"') {
                validateString(); //string
            } else if (current_char == '-' || (current_char >= 48 && current_char <= 57)) {
                validateNumber(); //number
            } else if (current_char == '{') {
                if (!validateObject()) { //object
                    return false;
                }
            } else if (current_char == '[') {
                if (!validateArray()) { //array
                    return false;
                }
            } else if (current_char == 't' || current_char == 'f' || current_char == 'n') {
                validateTFN(); //test the special value true/false/null
            } else {
                return false;
            }
            switch (nextRealChar()) {
                case ',':
                    nextRealChar(); //it still has other elements
                    continue;
                case ']':
                    return true; //no other elements
                default:
                    return false; //error char
            }
        }
    }

    //validate the object structure
    public static boolean validateObject() throws CustomException {
        nextRealChar();
        if (current_char == '}') {
            return true; //empty object, return true
        } else if (current_char == ','){
           throw new CustomException("Extra comma found"); //extra comma after '{'
        }
        while(true) {
            if (current_char == '}') {
                throw new CustomException("Extra comma found"); //extra comma, this is testing while it has iterations
            } else if (current_char == '"') {
                validateString(); //string key
            } else {
                return false;
            }
            if (nextRealChar() != ':') {
                return false;
            }
            nextRealChar(); //go to the value
            if (current_char == ',') {
                throw new CustomException("No values in key-value pair"); //No values in the pair
            } else if (current_char == '"') {
                validateString(); //string
            } else if (current_char == '-' || (current_char >= 48 && current_char <= 57)) {
                validateNumber(); //number
            } else if (current_char == '{') {
                if (!validateObject()) { //object
                    return false;
                }
            } else if (current_char == '[') {
                if (!validateArray()) {//array
                    return false;
                }
            } else if (current_char == 't' || current_char == 'f' || current_char == 'n') {
                validateTFN(); //test the special value true/false/null
            } else{
                return false;
            }
            switch (nextRealChar()) {
                case ',':
                    nextRealChar(); //it still has other elements
                    continue;
                case '}':
                    return true; //no other elements
                default:
                    return false; //error char
            }
        }
    }

    //validate the string structure
    public static void validateString() throws CustomException {
        do {
            current_char = nextChar();
            if (current_char == '\\') {
                if ("\"\\/bfnrtu".indexOf(nextChar()) < 0) {
                    throw new CustomException("Invalid escape char found"); //fixed char after '\' check
                }
                if (current_char == 'u') { //check unicode format 0-9 a-f A-F
                    for (int i = 0; i < 4; i++) {
                        nextChar(); // check unicode format
                        if (current_char < 48 || (current_char > 57 && current_char < 65) || (current_char > 70 && current_char < 97)
                                || current_char > 102) {
                            throw new CustomException("Invalid hex found");
                        }
                    }
                } else if(current_char == '"'){
                    nextChar(); //make the '\"' correct while it comes with the '"'
                }
            }
        } while (current_char >= 32 && current_char != 34 && current_char != 127);
        if (current_char == 0) {
            throw new CustomException("Unclosed quote found");
        } else if (current_char != 34) {
            throw new CustomException("Invalid string found");
        }
    }

    //validate the number structure
    public static void validateNumber() throws CustomException {
        if (current_char == '-') {
            current_char = nextChar(); //it can begin with '-'
        }
        if (current_char > 48 && current_char <= 57) {
            while(current_char >= 48 && current_char <= 57){
                current_char = nextChar(); //integer part cannot start with 0 and follow with other numbers
            }
        } else if (current_char == 48) {
            current_char = nextChar(); //integer part start with 0
        } else {
            throw new CustomException("Invalid number found");
        }
        if (current_char == '.') { //fraction
            current_char = nextChar();
            if (current_char >= 48 && current_char <= 57) {
                while (current_char >= 48 && current_char <= 57) {
                    current_char = nextChar(); //after '.', it can be 0-9
                }
            } else {
                throw new CustomException("Invalid number found");
            }
        }
        if (current_char == 'e' || current_char == 'E') { //exponent
            current_char = nextChar(); //scientific notation first has 'e'/'E'
            if (current_char == '+' || current_char == '-') {
                current_char = nextChar(); //scientific notation then has '-'/'+'
            }
            if (current_char < 48 || current_char > 57) {
                throw new CustomException("Invalid number found");
            }
            while (current_char >= 48 && current_char <= 57){
                current_char = nextChar(); //it can have any digits next
            }
        }
        array_pointer--;
    }
    
    //validate the special value true/false/null
    public static void validateTFN() throws CustomException {
        StringBuilder sb = new StringBuilder();
        do {
            sb.append(current_char);
            current_char = nextChar();
        } while (current_char >= ' ' && ",]#/".indexOf(current_char) < 0 && current_char != 127);
        if (!sb.toString().equals("true") && !sb.toString().equals("false") && !sb.toString().equals("null")) {
            throw new CustomException("Invalid true/false/null");
        }
        array_pointer--;
    }

    //Main function to validate the JSON file
    public static boolean isJSON(String input) {
        try {
            array_pointer = 0;
            string = input;
            //find the next real char as the entry of the validator
            switch (nextRealChar()) {
                case '[': //an array at the outset
                    if (nextRealChar() == ']') {
                        if (array_pointer < input.length() && nextRealChar() != input.charAt(array_pointer)) {
                            return false; //there are other chars after the outset array
                        }
                        return true; //there is nothing after the outset array
                    }
                    array_pointer--; //Go back one char and then go to the array validator
                    if(validateArray() == true){
                        if(array_pointer < input.length() && nextRealChar() != input.charAt(array_pointer)){
                            return false; //there are other chars after the outset array
                        }
                        return true; //there is nothing after the outset array
                    }
                    return false;
                case '{': //an object at the outset
                    if (nextRealChar() == '}') {
                        if (array_pointer < input.length() && nextRealChar() != input.charAt(array_pointer)) {
                            return false; //there are other chars after the outset object
                        }
                        return true; //there is nothing after the outset object
                    }
                    array_pointer--; //Go back one char and then go to the object validator part
                    if(validateObject() == true){
                        if(array_pointer < input.length() && nextRealChar() != input.charAt(array_pointer)){
                            return false; //there are other chars after the outset object
                        }
                        return true; //there is nothing after the outset object
                    }
                    return false;
                default:
                    return false;
            }
        } catch(Exception e){
            return false;
        }
    }

    public static void main(String[] args) throws IOException {
        int count_fail = 0;
        int count_pass = 0;
        System.out.println("For 33 fail test cases:");
        for(int i = 1; i <= 33 ; i++) {
            try{
                String input_string = readFileToString("test/fail" + i + ".json");
                if(JsonValidator.isJSON(input_string.trim()) == false){
                    count_fail ++ ;
                }
            }catch (Exception e){
                count_fail ++ ;
            }
        }
        System.out.println(count_fail + " fail and " + (33-count_fail) + " pass");
        System.out.println("For 3 pass test cases:");
        for(int i = 1; i <= 3 ; i++) {
            try{
                String input_string = readFileToString("test/pass" + i + ".json");
                if(JsonValidator.isJSON(input_string.trim()) == true){
                    count_pass ++ ;
                }
            }catch (Exception e){}
        }
        System.out.println(count_pass + " pass and " + (3-count_pass) + " fail");
        //Add one specific file here, just change the filepath
        System.out.println("For the given case:");
        String input_string = readFileToString("test.txt");
        System.out.println(JsonValidator.isJSON(input_string.trim()));
    }
}