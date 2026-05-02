package com.cookio.app.ai;

import android.text.TextUtils;
import android.util.Log;

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
    private static final String TAG = "AiRecipeHelper";

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
                        + "The recipe should be realistic, appetizing, and useful for everyday home cooks.\n"
                        + "It does not need to be especially cheap, student-focused, or ultra-simple unless the ingredients naturally suggest that.\n"
                        + "Return the result in this exact format:\n"
                        + "TITLE: ...\n"
                        + "DESCRIPTION: ...\n"
                        + "INGREDIENTS: item1 || item2 || item3\n"
                        + "STEPS: step1 instruction|minutes || step2 instruction|minutes || step3 instruction|minutes\n"
                        + "TIME: ...\n"
                        + "BUDGET: Low/Medium/High\n"
                        + "EQUIPMENT: tool1 || tool2 || tool3\n"
                        + "Use 0 minutes when a step does not need a timer.\n"
                        + "Only assign minutes to steps that involve actual cooking, baking, simmering, resting, chilling, freezing, marinating, or heating time.\n"
                        + "Do not assign time to quick prep actions like chopping, mixing, slicing, cracking, or seasoning, because that varies by person.\n"
                        + "Important: use || only between separate ingredients or equipment items.\n"
                        + "Do not split a single ingredient with commas. For example write \"1 medium onion, chopped\" as one ingredient item.";

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
                        Throwable root = unwrapThrowable(t);
                        Log.e(TAG, "AI generation failed", root);
                        callback.onError(new Exception(buildReadableMessage(root), root));
                    }
                },
                executor
        );
    }

    private Throwable unwrapThrowable(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private String buildReadableMessage(Throwable throwable) {
        String message = throwable == null ? "" : throwable.getMessage();
        if (TextUtils.isEmpty(message)) {
            return "Unknown Firebase AI error";
        }

        String lowered = message.toLowerCase();

        if (lowered.contains("permission") || lowered.contains("access denied")) {
            return "Firebase AI is not enabled for this project or the app does not have permission to use it.";
        }
        if (lowered.contains("quota") || lowered.contains("429")) {
            return "Firebase AI quota was exceeded. Check your plan, quotas, or billing setup.";
        }
        if (lowered.contains("payment credits are depleted")
                || lowered.contains("credits are depleted")
                || lowered.contains("insufficient balance")
                || lowered.contains("billing account")
                || lowered.contains("payment required")) {
            return "AI generation is unavailable because the Google/Firebase project billing balance is depleted or billing is not fully set up. Refill or attach a valid billing account, or switch this Firebase project back to a supported free-tier Gemini setup.";
        }
        if (lowered.contains("api key")) {
            return "Firebase AI setup is incomplete. Check the Gemini API provider setup in Firebase Console.";
        }
        if (lowered.contains("network") || lowered.contains("unable to resolve host")) {
            return "Network error while contacting Firebase AI.";
        }

        return message;
    }
}
