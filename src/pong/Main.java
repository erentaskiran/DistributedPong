package pong;


public class Main {
    public static void main(String[] args) {
        for (long i = 0; i<10000; i++){
            System.out.println(i);
            try {
                Thread.sleep(1000);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }
}
