package com.driver.services.impl;

import com.driver.model.*;
import com.driver.repository.ConnectionRepository;
import com.driver.repository.ServiceProviderRepository;
import com.driver.repository.UserRepository;
import com.driver.services.ConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
public class ConnectionServiceImpl implements ConnectionService {
    @Autowired
    UserRepository userRepository2;
    @Autowired
    ServiceProviderRepository serviceProviderRepository2;
    @Autowired
    ConnectionRepository connectionRepository2;

    @Override
    public User connect(int userId, String countryName) throws Exception{
        String countryNameInUpperCase = countryName.toUpperCase();
        CountryName countryName1 = CountryName.valueOf(countryNameInUpperCase);

        User user = userRepository2.findById(userId).get();
        if(user.getConnected()){
            throw new Exception("Already connected");
        }
        else if(user.getOriginalIp().equals(countryName1.toCode())){
            return user;
        }
        else {
            List<ServiceProvider> serviceProviderList = checkCountryInServiceProvider(user.getServiceProviderList(), countryName1.toCode());
            if (user.getServiceProviderList().isEmpty()||serviceProviderList.isEmpty()){
                throw new Exception("Unable to connect");
            }else {
                Collections.sort(serviceProviderList, new Comparator<ServiceProvider>() {
                    @Override
                    public int compare(ServiceProvider o1, ServiceProvider o2) {
                        return o1.getId() - o2.getId();
                    }
                });
                user.setMaskedIp(countryName1.toCode()+"."+serviceProviderList.get(0).getId()+"."+user.getId());
                user.setConnected(true);

                for (Country country: serviceProviderList.get(0).getCountryList()){
                    if(country.getCountryName().toCode().equals(countryName1.toCode())){
                        user.setOriginalCountry(country);
                    }
                }

                userRepository2.save(user);
            }
        }
        return user;

    }
    public List<ServiceProvider> checkCountryInServiceProvider(List<ServiceProvider> list,String countryCode){
        List<ServiceProvider> serviceProviderList = new ArrayList<>();
        for (ServiceProvider serviceProvider: list){
            for (Country country: serviceProvider.getCountryList()){
                if(country.getCountryName().toCode().equals(countryCode)){
                    serviceProviderList.add(serviceProvider);
                }
            }
        }
        return serviceProviderList;

    }
    @Override
    public User disconnect(int userId) throws Exception {
        User user = userRepository2.findById(userId).get();
        if(!user.getConnected()){
            throw new Exception("Already disconnected");
        }
        user.setConnected(false);
        user.setMaskedIp(null);

        String countryCode = user.getOriginalIp().substring(0,3);
        CountryName countryName = CountryName.valueOf(countryCode);
        Country country = new Country();
        country.setCountryName(countryName);
        country.setCode(countryCode);
        country.setUser(user);
        country.setServiceProvider(null);

        user.setOriginalCountry(country);

        userRepository2.save(user);
        return user;
    }
    @Override
    public User communicate(int senderId, int receiverId) throws Exception {
        User sender = userRepository2.findById(senderId).get();
        User recever = userRepository2.findById(receiverId).get();
        Country senderCountry = sender.getOriginalCountry();
        Country receverCountry = recever.getOriginalCountry();

        User updatedSender;
        if(senderCountry.getCountryName() != receverCountry.getCountryName()){
            try {
                updatedSender = connect(sender.getId(),receverCountry.getCountryName().toString());
            }catch (Exception e){
                throw new Exception("Cannot establish communication");
            }
        }else {
            return sender;
        }
        return updatedSender;

    }
}
