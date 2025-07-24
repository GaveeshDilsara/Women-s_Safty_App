package com.example.home;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.ExecutionException;

public class RecordingFragment extends Fragment {

    private PreviewView previewView;
    private VideoCapture<Recorder> videoCapture;
    private Recording recording;
    private Button recordButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recording, container, false);

        previewView = view.findViewById(R.id.preview_view);
        recordButton = view.findViewById(R.id.record_button);

        recordButton.setOnClickListener(v -> {
            if (recording != null) {
                recording.stop();
                recording = null;
                recordButton.setText("Start Recording");
            } else {
                startRecording();
            }
        });

        setupCamera();

        return view;
    }

    private void setupCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                Recorder recorder = new Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HD))
                        .build();
                videoCapture = VideoCapture.withOutput(recorder);

                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture);

            } catch (ExecutionException | InterruptedException e) {
                Log.e("RecordingFragment", "Camera initialization failed.", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void startRecording() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO});
        } else {
            File videoFile = new File(requireContext().getExternalFilesDir(null), "video.mp4");
            FileOutputOptions outputOptions = new FileOutputOptions.Builder(videoFile).build();

            recording = videoCapture.getOutput()
                    .prepareRecording(requireContext(), outputOptions)
                    .withAudioEnabled()
                    .start(ContextCompat.getMainExecutor(requireContext()), videoRecordEvent -> {
                        if (videoRecordEvent instanceof VideoRecordEvent.Start) {
                            Toast.makeText(requireContext(), "Recording started", Toast.LENGTH_SHORT).show();
                            recordButton.setText("Stop Recording");
                        } else if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                            Uri savedUri = ((VideoRecordEvent.Finalize) videoRecordEvent).getOutputResults().getOutputUri();
                            if (savedUri != null) {
                                Toast.makeText(requireContext(), "Video saved: " + savedUri, Toast.LENGTH_SHORT).show();
                            }
                            recordButton.setText("Start Recording");
                        }
                    });
        }
    }

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                Boolean cameraGranted = permissions.getOrDefault(Manifest.permission.CAMERA, false);
                Boolean audioGranted = permissions.getOrDefault(Manifest.permission.RECORD_AUDIO, false);

                if (cameraGranted != null && cameraGranted && audioGranted != null && audioGranted) {
                    startRecording();
                } else {
                    Toast.makeText(requireContext(), "Camera or audio permission denied", Toast.LENGTH_SHORT).show();
                }
            });
}
