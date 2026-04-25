package com.cookio.app.ai;

import androidx.annotation.NonNull;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.GenerativeModel;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerateContentResponse;
import com.google.firebase.ai.type.GenerativeBackend;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AiRecipeHelper {

    private final GenerativeModelFutures model;
    private final Executor executor = Executors.newSingleThreadExecutor();

    public interface AiRecipeCallback {
        void onSuccess(String result);
        void onError(Exception e);
    }

    public AiRecipeHelper() {
        GenerativeModel generativeModel = FirebaseAI.getInstance(GenerativeBackend.googleAI())
                .generativeModel("gemini-2.5-flash");

        model = GenerativeModelFutures.from(generativeModel);
    }

    public void generateRecipe(String ingredients, AiRecipeCallback callback) {
        String prompt =
                "Generate a simple cooking social media recipe post using these ingredients: "
                        + ingredients + "\n\n"
                        + "The recipe should be beginner-friendly, affordable, and realistic.\n"
                        + "Return the result in this exact format:\n"
                        + "TITLE: ...\n"
                        + "DESCRIPTION: ...\n"
                        + "INGREDIENTS: item1, item2, item3\n"
                        + "STEPS: step1 | step2 | step3\n"
                        + "TIME: ...\n"
                        + "BUDGET: Low/Medium/High\n"
                        + "EQUIPMENT: ...";

        Content content = new Content.Builder()
                .addText(prompt)
                .build();

        Futures.addCallback(
                model.generateContent(content),
                new FutureCallback<GenerateContentResponse>() {
                    @Override
                    public void onSuccess(GenerateContentResponse response) {
                        if (response != null && response.getText() != null) {
                            callback.onSuccess(response.getText());
                        } else {
                            callback.onError(new Exception("Empty AI response"));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        callback.onError(new Exception(t));
                    }
                },
                executor
        );
    }
}
