/******************************************************************************
 * e-voting system                                                            *
 * Copyright (C) 2016 DSX Technologies Limited.                               *
 * *
 * This program is free software; you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation; either version 2 of the License, or          *
 * (at your option) any later version.                                        *
 * *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied                         *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * *
 * You can find copy of the GNU General Public License in LICENSE.txt file    *
 * at the top-level directory of this distribution.                           *
 * *
 * Removal or modification of this copyright notice is prohibited.            *
 * *
 ******************************************************************************/

package uk.dsxt.voting.common.utils;

import lombok.AllArgsConstructor;
import lombok.Value;
import uk.dsxt.voting.common.datamodel.InternalLogicException;
import uk.dsxt.voting.common.datamodel.RequestType;

import javax.ws.rs.core.Response;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

@Value
@AllArgsConstructor
public class HttpHelper {
    int connectionTimeout;
    int readTimeout;

    public String request(String urlString, RequestType type) throws IOException, InternalLogicException {
        return request(urlString, (String)null, type);
    }

    public String request(String urlString, String content, RequestType type) throws IOException, InternalLogicException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(type.toString());
        connection.setRequestProperty("Content-type", "application/json");
        connection.setConnectTimeout(connectionTimeout);
        connection.setReadTimeout(readTimeout);

        if (content != null && type == RequestType.POST) {
            connection.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(content);
            wr.flush();
            wr.close();
        }

        int code = connection.getResponseCode();
        if (code != Response.Status.OK.getStatusCode())
            throw new InternalLogicException(String.format("request failed. code %s for url %s", code, urlString));

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        return response.toString();
    }

    public String request(String urlString, Map<String, String> parameters, RequestType type) throws IOException, InternalLogicException {
        return request(urlString, buildContent(parameters), type);
    }

    private String buildContent(Map<String, String> parameters) throws UnsupportedEncodingException {
        if (parameters == null)
            return "";

        StringBuilder paramString = new StringBuilder();
        for (Map.Entry<String, String> param : parameters.entrySet()) {
            if (param.getValue() == null)
                continue;
            if (paramString.length() > 0)
                paramString.append('&');
            paramString.append(param.getKey());
            paramString.append('=');
            paramString.append(URLEncoder.encode(param.getValue(), "UTF-8"));
        }
        return paramString.toString();
    }
}
