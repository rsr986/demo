package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.services.sts.model.StsException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {

        System.out.println("Hello, World!");
        String region = "ap-south-1";
        String s3BucketName = "elasticbeanstalk-ap-south-1-082837426421";

        StsClient stsClient = StsClient.builder()
                .region(Region.of(region))
                .build();

        assumeGivenRole(stsClient);

        S3Client s3 = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(SystemPropertyCredentialsProvider.create())
                .build();
        listBucketObjects(s3, s3BucketName);

        stsClient.close();
        System.out.println("Hello, World!");
    }


    public static void assumeGivenRole(StsClient stsClient) {
        String roleArn = "arn:aws:iam::082837426421:role/test-role";
        String roleSessionName = "mysession";

        try {
            AssumeRoleRequest roleRequest = AssumeRoleRequest.builder()
                    .roleArn(roleArn)
                    .roleSessionName(roleSessionName)
                    .build();
            System.out.println("----------- After roleRequest ------------");
            AssumeRoleResponse roleResponse = stsClient.assumeRole(roleRequest);
            System.out.println("----------------    After roleResponse ---------");
            Credentials myCreds = roleResponse.credentials();
            System.out.println( "access_key: " + myCreds.accessKeyId() + " serect: " + myCreds.secretAccessKey() + " session_token: " + myCreds.sessionToken());
            System.out.println("---------------  After credentials");
            // Display the time when the temp creds expire
            Instant exTime = myCreds.expiration();
            String tokenInfo = myCreds.sessionToken();

            // Convert the Instant to readable date
            DateTimeFormatter formatter =
                    DateTimeFormatter.ofLocalizedDateTime( FormatStyle.SHORT )
                            .withLocale( Locale.US)
                            .withZone( ZoneId.systemDefault() );

            formatter.format( exTime );
            System.out.println("The token "+tokenInfo + "  expires on " + exTime );

            System.setProperty("aws.accessKeyId", myCreds.accessKeyId());
            System.setProperty("aws.secretAccessKey", myCreds.secretAccessKey());
            System.setProperty("aws.sessionToken", myCreds.sessionToken());

        } catch (StsException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    public static void listBucketObjects(S3Client s3, String bucketName ) {

        try {
            ListObjectsRequest listObjects = ListObjectsRequest
                    .builder()
                    .bucket(bucketName)
                    .build();

            ListObjectsResponse res = s3.listObjects(listObjects);
            List<S3Object> objects = res.contents();

            for (ListIterator iterVals = objects.listIterator(); iterVals.hasNext(); ) {
                S3Object myValue = (S3Object) iterVals.next();
                System.out.print("\n The name of the key is " + myValue.key());
                System.out.print("\n The object is " + calKb(myValue.size()) + " KBs");
                System.out.print("\n The owner is " + myValue.owner());
            }

        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }
    //convert bytes to kbs
    private static long calKb(Long val) {
        return val/1024;
    }
}
