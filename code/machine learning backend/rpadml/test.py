from tldextract import extract

def sub_domain(url):
    subDomain, domain, suffix = extract(url)
    print(subDomain)
    if(subDomain.count('.')==0):
        return -1
    elif(subDomain.count('.')==1):
        return 0
    else:
        return 1


print(sub_domain("https://facebook-account-support-confirmation.buy.weebly.com/"))