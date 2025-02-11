package burp.adaptive.learning.ai;

import burp.adaptive.learning.LearningExtension;
import burp.api.montoya.http.handler.HttpRequestToBeSent;
import burp.api.montoya.http.handler.HttpResponseReceived;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.PrintWriter;
import java.io.StringWriter;

import static burp.adaptive.learning.LearningExtension.*;

public class VariationAnalyser {
    public static void analyse(JSONArray headersAndParameters, HttpRequestToBeSent req, HttpResponseReceived[] repeaterResponses) {
        LearningExtension.executorService.submit(() -> {
            try {
                api.logging().logToOutput("------");
                api.logging().logToOutput("Analysing:\n" + headersAndParameters.toString() + "\n");
                AI ai = new AI();
                ai.setBypassRateLimit(true);
                ai.setSystemMessage("""
                        You are a web security expert.
                        Your job is to analyze the JSON given to you and look for variations of what's being tested.
                        You should return a JSON array of""" + " " + amountOfVariations + " " + """
                        The JSON structure should be:[{"vector":"$yourVariation"}].
                        If you cannot find a variation just return an empty array.
                        Do not output markdown. Do not describe what you are doing just return JSON.
                        Your response should be a valid JSON array that conforms to the JSON specification.
                        Validate your JSON response and ensure it's valid JSON.
                        You should correctly escape all strings in the JSON value.
                        You should be creative and imagine a WAF blocking the vector and come up with creative ways of bypassing it.
                        Here is a list of headers and parameters for you to analyse in JSON:
                        """);

                ai.setPrompt(headersAndParameters.toString());
                ai.setTemperature(1.0);
                String response = ai.execute();
                try {
                    JSONArray variations = new JSONArray(response);
                    api.logging().logToOutput("Variations found:\n" + variations);
                    api.logging().logToOutput("------");
                    OrganiseVectors.organise(req, variations, headersAndParameters, repeaterResponses);
                } catch (JSONException e) {
                    api.logging().logToError("The AI returned invalid JSON");
                }
            } catch (Throwable throwable) {
                StringWriter writer = new StringWriter();
                throwable.printStackTrace(new PrintWriter(writer));
                api.logging().logToError(writer.toString());
            }
        });
    }
}
