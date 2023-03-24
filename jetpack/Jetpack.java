package jetpack;

import static javafx.application.Application.launch;


import java.util.*;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.concurrent.atomic.*;

/**
 *
 * @author rdngrl05a04h501o
 */
public class Jetpack extends Application {
    private final double GRAVITY = 9.81;
    private final double SPEED = 500;
    private int ENEMY_SPAWN_DELAY = 500;
    
    private ArrayList<Sprite> enemies = new ArrayList<>();
    private int score = 0;

    
    private final double multiplier = 1.6;
    private final double dimensions[] = getDimensions();
    
    private Canvas canvas;
    private final double width = dimensions[0];
    private final double height = dimensions[1];
    
    // player sprite
    private Sprite player = new Sprite();
    
    
    
    public static void main(String[] args) 
    {
        launch(args);
    }
    
    @Override
    public void start(Stage stage) throws Exception {
    
        // initialize player sprite
        player.setImage("jetpack/images/idle.png", multiplier);
        player.setPosition(100, 100);
    
        // set a timer for enemy spawning
        Timer timer1 = new Timer();
        timer1.schedule(new TimerTask() {
            @Override
            public void run() {
                spawnEnemies();
                removeOffscreenEnemies();
            }
        }, 1000, ENEMY_SPAWN_DELAY);
        
            
        AtomicBoolean keyWait = new AtomicBoolean(true); // used on key release
        
        stage.setOnCloseRequest((WindowEvent event) -> {
            System.exit(0);
        });
        
        /* dichiara canvas   */
        stage.setTitle( "Gioco" );

        Group root = new Group();
        Scene scene = new Scene( root );
        stage.setScene( scene );

        canvas = new Canvas( width, height );
        root.getChildren().add( canvas );
        stage.setResizable(false);
        
        
        // ArrayList contenente gli input da tastiera (queue)
        ArrayList<String> input = new ArrayList<>();
        
        // gestione input
        scene.setOnKeyTyped(new EventHandler<KeyEvent>(){
                public void handle(KeyEvent e){
                    if (e.getCharacter().equals(" ") ) {
                        if ( !input.contains("SPACE")){
                            input.add( "SPACE" );
                        }
                    }    
                }
            });
        scene.setOnKeyPressed(
            new EventHandler<KeyEvent>(){
                public void handle(KeyEvent e){
                    String code = e.getCode().toString();
                    if ( !input.contains(code) && !code.equals("SPACE") )
                        input.add( code );
                }
            });

        scene.setOnKeyReleased(
            new EventHandler<KeyEvent>(){
                public void handle(KeyEvent e){
                    String code = e.getCode().toString();
                    input.remove( code );
                    keyWait.set(true);
                }
            });
        
        
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, width, height);
        
        
        Sprite background = new Sprite();
        background.setImage("jetpack/images/bg.png", multiplier);
        background.setPosition(0, 0);
        
        // Create a font for the text
        Font font = Font.font("Arial", FontWeight.BOLD, 24);

        // Set the font for the graphics context
        gc.setFont(font);

        // Set the fill color for the text
        gc.setFill(Color.BLACK);


        // hello BOY
        LongValue lastNanoTime = new LongValue(System.nanoTime());
        
        score = 1;
        new AnimationTimer()
        {
            
            boolean isPlayerAlive = true;
            private long lastUpdate = 0;
            
            @Override
            public void handle(long currentNanoTime)
            {
                
                if(!isPlayerAlive){
                    this.stop();
                }
                    
                double elapsedTime = (currentNanoTime - lastNanoTime.value) / 1000000000.0;
                lastNanoTime.value = currentNanoTime;
                
                if (currentNanoTime - lastUpdate >= 100000000) { // Update score every 10ms
                    score++;
                    lastUpdate = currentNanoTime;
                }
                
                
                if (input.contains("UP")){
                    if(keyWait.get() == true){
                        
                        // jump!
                        fly(player);
                    }
                }
                
                // game logic -->
                if(isColliding(player) != true){
                    
                    if(isOnFloor(player) == true){
                        // ON FLOOR
                        player.setVelocity(0, 0);
                        riposiziona(player);
                        player.setImage("jetpack/images/idle.png", multiplier);
                    }else{
                        if(input.isEmpty() == true){
                            // IN AIR - IDLE
                            player.setImage("jetpack/images/idle.png", multiplier);
                            fall(player);
                        }else{
                            // IN AIR - FLY
                            player.setImage("jetpack/images/air.png", multiplier);
                        }
                    }
                    
                }
                
                else{
                    isPlayerAlive = false;
                }
                
                
                
                // render background
                background.render(gc);
                
                // render player sprite
                player.update(elapsedTime);
                player.render(gc);
                
                // render enemies sprites
                for (Sprite enemy : enemies) {
                    enemy.update(elapsedTime);
                    enemy.render(gc);
                }
                
                // Get the current score and convert it to a string
                String scoreStr = Integer.toString(score);

                // Draw the score text at position (x, y)
                gc.fillText("Score: " + scoreStr, 50, 50);
            }
        }.start();
        
        
        stage.show();
    }
    
    private boolean isOnFloor(Sprite sprite){
        return sprite.getY() > height - (height/5);
    }
    
    private void fly(Sprite sprite){
        // Apply reverse gravity to the sprite's velocity
        double vy = sprite.getVelocityY() - GRAVITY/1.5 * 0.1;

        // Update the sprite's velocity
        sprite.setVelocityY(vy);

        // Update the sprite's position based on its new velocity
        double x = sprite.getX() + sprite.getVelocityX();
        double y = sprite.getY() + sprite.getVelocityY();

        // Check if the new position is above the top of the window
        if (y < 0) {
            y = 0;
            sprite.setVelocityY(0);
        }

        sprite.setPosition(x, y);
    }

    
    private void fall(Sprite sprite) {
        // Apply gravity to the sprite's velocity
        double vy = sprite.getVelocityY() + GRAVITY * 0.1;

        // Update the sprite's velocity
        sprite.setVelocityY(vy);

        // Update the sprite's position based on its new velocity
        double x = sprite.getX() + sprite.getVelocityX();
        double y = sprite.getY() + sprite.getVelocityY();
        sprite.setPosition(x, y);
    }

    
    private void riposiziona(Sprite sprite){
        sprite.setPosition(sprite.getX(), height - height/5);
    }
    
    private boolean isColliding(Sprite player) {
        for (Sprite enemy : enemies) {
            if (player.intersects(enemy)) {
                return true;
            }
        }
        return false;
    }

    private void spawnEnemies() {
        // Create a new enemy sprite with an image and a random Y position
        Sprite enemy = new Sprite();
        enemy.setImage("jetpack/images/sfera.png", multiplier);

        // Set enemy's Y position to a random value between player's Y position +/- 500 pixels
        double playerY = player.getY();
        double enemyY = playerY + (Math.random() * 1000) - 500;
        enemyY = Math.max(0, Math.min(enemyY, height - enemy.getHeight())); // Ensure enemy stays within screen bounds
        enemy.setPosition(width + enemy.getWidth(), enemyY); // Set enemy's X position just off the right side of the screen

        // Set enemy's velocity
        int spawnDelay = (int)(ENEMY_SPAWN_DELAY - (score / 100) * 100); // Adjust spawn rate based on score
        enemy.setVelocityX(-SPEED);

        // Add the enemy sprite to the list of sprites
        enemies.add(enemy);
    }




    private void removeOffscreenEnemies() {
        Iterator<Sprite> iterator = enemies.iterator();
        while (iterator.hasNext()) {
            Sprite enemy = iterator.next();
            if (enemy.getX() + enemy.getWidth() < 0) {
                iterator.remove();
            }
        }
    }

    
    // get dimension for the window
    private double[] getDimensions(){
        
        Image tmp = new Image("jetpack/images/bg.png");
        double arr[] = new double[2];
        arr[0] = tmp.getWidth() * multiplier;
        arr[1] = tmp.getHeight() * multiplier;
        
        return arr;
    }
}