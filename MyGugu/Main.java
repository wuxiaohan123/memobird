package MyGugu;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application
{
	@Override
	public void start(Stage primaryStage)
	{
		VBox pane = new VBox();
		pane.setAlignment(Pos.CENTER);
		
		TextArea taNote = new TextArea("请输入文本，并确保最后一个字符不是标点");
		taNote.setPrefColumnCount(16);
		taNote.setWrapText(true);
		taNote.setEditable(true);
		//taNote.setFont(Font.font(""));
		
		ScrollPane scrollPane = new ScrollPane(taNote);
		
		Button button = new Button("Print");
		ButtonHandlerClass handler = new ButtonHandlerClass();
		button.setOnAction(handler);
		handler.setTextArea(taNote);
		
		pane.getChildren().addAll(taNote,scrollPane,button);
		
		Scene scene = new Scene(pane);
		primaryStage.setTitle("Print");
		primaryStage.setScene(scene);
		primaryStage.setResizable(false);
		primaryStage.show();
	}
	
	
	
	public static void main(String[] args)
	{
		Application.launch(args);
	}
}

class ButtonHandlerClass implements EventHandler<ActionEvent>
{
	private TextArea taNote;
	
	@Override
	public void handle(ActionEvent e)
	{
		new MemobirdAPI().printText(taNote.getText());
	}
	
	public void setTextArea(TextArea taNote)
	{
		this.taNote = taNote;
	}
}
