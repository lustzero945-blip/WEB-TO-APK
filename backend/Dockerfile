FROM python:3.10-slim

# Install system dependencies, OpenJDK 17, and unzip
RUN apt-get update && apt-get install -y --no-install-recommends \
    openjdk-17-jdk-headless \
    unzip \
    wget \
    git \
    curl \
    libpcre3 \
    libpcre3-dev \
    build-essential \
    libpq-dev \
    && rm -rf /var/lib/apt/lists/*

# Set Java Home details
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH=$PATH:$JAVA_HOME/bin

# (Optional) Pre-configure basic Android SDK directory hooks
ENV ANDROID_HOME=/storage/android-sdk
ENV PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# Prepare App runtime workspace
WORKDIR /workspace

# Install Python requirements
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy source repository into workspace container
COPY . .

# Create primary media, keys, templates, and builds output storage structure
RUN mkdir -p /storage/apk_builds /storage/uploaded_icons /storage/keystores /storage/templates

EXPOSE 8000
